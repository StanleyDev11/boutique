package com.example.boutique.controller;

import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.LigneVenteRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.VenteRepository;
import com.example.boutique.service.PdfGenerationService;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.opencsv.CSVWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/rapports")
public class RapportController {

    private final ProduitRepository produitRepository;
    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final PdfGenerationService pdfGenerationService;
    private static final int SEUIL_STOCK_BAS = 10;
    private static final int JOURS_AVANT_PEREMPTION = 30;

    public RapportController(ProduitRepository produitRepository, VenteRepository venteRepository, LigneVenteRepository ligneVenteRepository, PdfGenerationService pdfGenerationService, TemplateEngine templateEngine) {
        this.produitRepository = produitRepository;
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping("/stock-bas")
    public String rapportStockBas(Model model,
                                  @RequestParam(required = false) String filter,
                                  @RequestParam(defaultValue = "asc") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "8") int size) {

        // --- Préparation de la Pagination et du Tri ---
        Sort.Direction direction = "desc".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "quantiteEnStock"));

        // --- Récupération des Données Paginées pour l'affichage ---
        Page<Produit> produitsPage;
        if ("rupture".equals(filter)) {
            produitsPage = produitRepository.findAllByQuantiteEnStock(0, pageable);
        } else {
            produitsPage = produitRepository.findAllByQuantiteEnStockLessThanEqual(SEUIL_STOCK_BAS, pageable);
        }

        // --- Calcul des Statistiques Globales (non paginées) ---
        List<Produit> tousLesProduitsEnStockBas = produitRepository.findAllByQuantiteEnStockLessThanEqual(SEUIL_STOCK_BAS, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        long nombreEnRupture = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getQuantiteEnStock() == 0)
                .count();

        double valeurStockBas = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getPrixAchat() != null)
                .map(p -> p.getPrixAchat().multiply(new BigDecimal(p.getQuantiteEnStock())))
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        // --- Données de Péremption ---
        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateLimite = aujourdhui.plusDays(JOURS_AVANT_PEREMPTION);
        List<Produit> produitsPeremptionProche = produitRepository.findAllByDatePeremptionBetween(aujourdhui, dateLimite);

        // --- Ajout des données au modèle ---
        model.addAttribute("produitsPage", produitsPage);
        model.addAttribute("seuil", SEUIL_STOCK_BAS);
        model.addAttribute("sort", sort);
        model.addAttribute("activeFilter", filter);

        // Ajout des stats au modèle
        model.addAttribute("nombreStockBas", tousLesProduitsEnStockBas.size());
        model.addAttribute("nombreEnRupture", nombreEnRupture);
        model.addAttribute("valeurStockBas", valeurStockBas);
        model.addAttribute("produitsPeremptionProche", produitsPeremptionProche);
        model.addAttribute("joursAvantPeremption", JOURS_AVANT_PEREMPTION);
        model.addAttribute("totalProduits", produitRepository.count());

        return "rapport-stock-bas";
    }

    @GetMapping("/ventes-historique")
    public String ventesHistorique(Model model,
                                   @RequestParam(required = false) String nomProduit,
                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {

        Sort sort = Sort.by(Sort.Direction.DESC, "dateVente");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<com.example.boutique.model.Vente> ventePage;

        // Note: Filtering by product name across sales is more complex now.
        // This basic implementation will just fetch all sales.
        // A more advanced implementation would require custom queries.
        if (startDate != null && endDate != null) {
            ventePage = venteRepository.findByDateVenteBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable);
        } else {
            ventePage = venteRepository.findAll(pageable);
        }

        // Simplified metrics for the new view
        BigDecimal totalRevenue = venteRepository.findAll().stream()
                .filter(v -> v.getStatus() == com.example.boutique.enums.VenteStatus.COMPLETED)
                .map(com.example.boutique.model.Vente::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalSales = venteRepository.countByStatus(com.example.boutique.enums.VenteStatus.COMPLETED);
        BigDecimal todaysRevenue = venteRepository.findAllByDateVenteAfter(LocalDate.now().atStartOfDay()).stream()
                .filter(v -> v.getStatus() == com.example.boutique.enums.VenteStatus.COMPLETED)
                .map(com.example.boutique.model.Vente::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        model.addAttribute("ventesPage", ventePage);
        model.addAttribute("produits", produitRepository.findAll(Sort.by("nom")));
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("todaysRevenue", todaysRevenue);
        model.addAttribute("revenueChange", 0.0); // Simplified for now
        model.addAttribute("salesChange", 0.0); // Simplified for now
        model.addAttribute("todayRevenueChange", 0.0); // Simplified for now
        
        // Most sold products logic can remain as is
        List<Object[]> mostSoldProductsRaw = ligneVenteRepository.findMostSoldProducts();
        long totalItemsSold = mostSoldProductsRaw.stream().mapToLong(item -> (long) item[1]).sum();
        List<Map<String, Object>> mostSoldProducts = mostSoldProductsRaw.stream()
                .map(item -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("produit", item[0]);
                    productMap.put("quantite", item[1]);
                    if (totalItemsSold > 0) {
                        double percentage = ((long) item[1] * 100.0) / totalItemsSold;
                        productMap.put("percentage", percentage);
                    } else {
                        productMap.put("percentage", 0.0);
                    }
                    return productMap;
                })
                .collect(Collectors.toList());
        model.addAttribute("mostSoldProducts", mostSoldProducts);

        return "ventes-historique";
    }

    @GetMapping("/ventes/imprimer")
    public String imprimerVentes(Model model,
                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(1);
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        if (startDate == null) {
            startDate = LocalDate.now().minusYears(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        org.springframework.data.domain.Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");
        List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort);

        // --- Regroupement par jour ---
        Map<LocalDate, List<com.example.boutique.model.Vente>> ventesParJour = ventes.stream()
                .collect(Collectors.groupingBy(v -> v.getDateVente().toLocalDate()));

        // --- Calcul des sous-totaux par jour ---
        Map<LocalDate, BigDecimal> sousTotauxParJour = ventes.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getDateVente().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, com.example.boutique.model.Vente::getTotalFinal, BigDecimal::add)
                ));

        // --- Calculs pour le résumé amélioré ---
        BigDecimal totalGeneral = ventes.stream()
                .map(com.example.boutique.model.Vente::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int nombreTotalVentes = ventes.size();

        BigDecimal panierMoyen = BigDecimal.ZERO;
        if (nombreTotalVentes > 0) {
            panierMoyen = totalGeneral.divide(new BigDecimal(nombreTotalVentes), 2, java.math.RoundingMode.HALF_UP);
        }

        Map<String, BigDecimal> totalParMoyenPaiement = ventes.stream()
                .collect(Collectors.groupingBy(
                        com.example.boutique.model.Vente::getMoyenPaiement,
                        Collectors.reducing(BigDecimal.ZERO, com.example.boutique.model.Vente::getTotalFinal, BigDecimal::add)
                ));


        model.addAttribute("ventesParJour", ventesParJour);
        model.addAttribute("sousTotauxParJour", sousTotauxParJour);
        model.addAttribute("totalGeneral", totalGeneral);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("nombreTotalVentes", nombreTotalVentes);
        model.addAttribute("panierMoyen", panierMoyen);
        model.addAttribute("totalParMoyenPaiement", totalParMoyenPaiement);
        model.addAttribute("dateGeneration", LocalDateTime.now());


        return "rapport-ventes";
    }

    @GetMapping("/sales-by-day")
    @ResponseBody
    public Map<String, Object> getSalesByDay() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);
        List<LigneVente> sales = ligneVenteRepository.findByVenteDateVenteBetween(startDate, endDate, Pageable.unpaged()).getContent();

        Map<LocalDate, BigDecimal> salesByDay = sales.stream()
                .collect(Collectors.groupingBy(lv -> lv.getVente().getDateVente().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, LigneVente::getMontantTotal, BigDecimal::add)));

        List<String> labels = salesByDay.keySet().stream().map(LocalDate::toString).collect(Collectors.toList());
        List<BigDecimal> data = new ArrayList<>(salesByDay.values());

        return Map.of("labels", labels, "data", data);
    }

    @GetMapping("/sales-by-category")
    @ResponseBody
    public Map<String, Object> getSalesByCategory() {
        List<LigneVente> sales = ligneVenteRepository.findAll();

        Map<String, BigDecimal> salesByCategory = sales.stream()
                .collect(Collectors.groupingBy(lv -> lv.getProduit().getCategorie(),
                        Collectors.reducing(BigDecimal.ZERO, LigneVente::getMontantTotal, BigDecimal::add)));

        List<String> labels = new ArrayList<>(salesByCategory.keySet());
        List<BigDecimal> data = new ArrayList<>(salesByCategory.values());

        return Map.of("labels", labels, "data", data);
    }

    @GetMapping("/export/csv")
    @ResponseBody
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; file=ventes.csv");

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.getOutputStream()))) {
            // Write header
            String[] header = {"ID Vente", "Produit", "Quantité", "Prix Unitaire", "Montant Total", "Date de Vente"};
            writer.writeNext(header);

            // Write data
            List<LigneVente> sales = ligneVenteRepository.findAllWithVente(Pageable.unpaged()).getContent();
            for (LigneVente sale : sales) {
                String[] data = {
                        String.valueOf(sale.getVente().getId()),
                        sale.getProduit().getNom(),
                        String.valueOf(sale.getQuantite()),
                        String.valueOf(sale.getPrixUnitaire()),
                        String.valueOf(sale.getMontantTotal()),
                        String.valueOf(sale.getVente().getDateVente())
                };
                writer.writeNext(data);
            }
        }
    }

    @GetMapping("/export/pdf")
    @ResponseBody
    public void exportPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; file=ventes.pdf");

        try (PdfWriter writer = new PdfWriter(response.getOutputStream());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            document.add(new Paragraph("Historique des Ventes"));

            Table table = new Table(6);
            table.addHeaderCell("ID Vente");
            table.addHeaderCell("Produit");
            table.addHeaderCell("Quantité");
            table.addHeaderCell("Prix Unitaire");
            table.addHeaderCell("Montant Total");
            table.addHeaderCell("Date de Vente");

            List<LigneVente> sales = ligneVenteRepository.findAll();
            for (LigneVente sale : sales) {
                table.addCell(String.valueOf(sale.getVente().getId()));
                table.addCell(sale.getProduit().getNom());
                table.addCell(String.valueOf(sale.getQuantite()));
                table.addCell(String.valueOf(sale.getPrixUnitaire()));
                table.addCell(String.valueOf(sale.getMontantTotal()));
                table.addCell(String.valueOf(sale.getVente().getDateVente()));
            }

                        document.add(table);

                    }

                }

            

                @GetMapping("/ventes/export/excel")

                public void exportVentesRapportExcel(HttpServletResponse response,

                                                     @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,

                                                     @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

            

                    LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(1);

                    LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

            

                    org.springframework.data.domain.Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");

                    List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort);

            

                    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

                    response.setHeader("Content-Disposition", "attachment; filename=rapport_ventes.xlsx");

            

                    try (XSSFWorkbook workbook = new XSSFWorkbook()) {

                        XSSFSheet sheet = workbook.createSheet("Rapport des Ventes");

            

                        // Create header row

                        Row headerRow = sheet.createRow(0);

                        String[] headers = {"ID Vente", "Date", "Client", "Caissier", "Moyen Paiement", "ID Produit", "Nom Produit", "Catégorie", "Quantité", "Prix Unitaire", "Total Ligne"};

                        for (int i = 0; i < headers.length; i++) {

                            Cell cell = headerRow.createCell(i);

                            cell.setCellValue(headers[i]);

                        }

            

                        // Create data rows

                        int rowNum = 1;

                        for (com.example.boutique.model.Vente vente : ventes) {

                            for (LigneVente ligne : vente.getLigneVentes()) {

                                Row row = sheet.createRow(rowNum++);

                                row.createCell(0).setCellValue(vente.getId());

                                row.createCell(1).setCellValue(vente.getDateVente().toString());

                                row.createCell(2).setCellValue(vente.getClient() != null ? vente.getClient().getNom() : "Client de passage");

                                row.createCell(3).setCellValue(vente.getUtilisateur().getUsername());

                                row.createCell(4).setCellValue(vente.getMoyenPaiement());

                                row.createCell(5).setCellValue(ligne.getProduit().getId());

                                row.createCell(6).setCellValue(ligne.getProduit().getNom());

                                row.createCell(7).setCellValue(ligne.getProduit().getCategorie());

                                row.createCell(8).setCellValue(ligne.getQuantite());

                                row.createCell(9).setCellValue(ligne.getPrixUnitaire().doubleValue());

                                row.createCell(10).setCellValue(ligne.getMontantTotal().doubleValue());

                            }

                        }

            

                                    workbook.write(response.getOutputStream());

            

                                }

            

                            }

            

                        

            

                            @GetMapping("/ventes/export/pdf")

            

                            public ResponseEntity<byte[]> exportRapportVentesPdf(@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,

            

                                                                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

            

                        

            

                                LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.now().minusYears(1);

            

                                LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

            

                        

            

                                if (startDate == null) {

            

                                    startDate = LocalDate.now().minusYears(1);

            

                                }

            

                                if (endDate == null) {

            

                                    endDate = LocalDate.now();

            

                                }

            

                        

            

                                org.springframework.data.domain.Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");

            

                                List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort);

            

                        

            

                                Map<String, Object> data = new HashMap<>();

            

                                data.put("ventesParJour", ventes.stream().collect(Collectors.groupingBy(v -> v.getDateVente().toLocalDate())));

            

                                data.put("sousTotauxParJour", ventes.stream().collect(Collectors.groupingBy(v -> v.getDateVente().toLocalDate(), Collectors.reducing(BigDecimal.ZERO, com.example.boutique.model.Vente::getTotalFinal, BigDecimal::add))));

            

                                BigDecimal totalGeneral = ventes.stream().map(com.example.boutique.model.Vente::getTotalFinal).reduce(BigDecimal.ZERO, BigDecimal::add);

            

                                data.put("totalGeneral", totalGeneral);

            

                                data.put("startDate", startDate);

            

                                data.put("endDate", endDate);

            

                                data.put("nombreTotalVentes", ventes.size());

            

                                data.put("panierMoyen", ventes.isEmpty() ? BigDecimal.ZERO : totalGeneral.divide(new BigDecimal(ventes.size()), 2, java.math.RoundingMode.HALF_UP));

            

                                data.put("totalParMoyenPaiement", ventes.stream().collect(Collectors.groupingBy(com.example.boutique.model.Vente::getMoyenPaiement, Collectors.reducing(BigDecimal.ZERO, com.example.boutique.model.Vente::getTotalFinal, BigDecimal::add))));

            

                                data.put("dateGeneration", LocalDateTime.now());

            

                        

            

                                byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("rapport-ventes", data);

            

                        

            

                                HttpHeaders headers = new HttpHeaders();

            

                                headers.setContentType(MediaType.APPLICATION_PDF);

            

                                headers.setContentDispositionFormData("attachment", "rapport-ventes.pdf");

            

                        

            

                                return ResponseEntity.ok().headers(headers).body(pdfBytes);

            

                            }

            

                        }

            

                        

            