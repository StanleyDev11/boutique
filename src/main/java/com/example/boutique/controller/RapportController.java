package com.example.boutique.controller;

import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.LigneVenteRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.VenteRepository;
import com.example.boutique.service.ParametreService;
import com.example.boutique.service.PdfGenerationService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final Logger logger = LoggerFactory.getLogger(RapportController.class);

    private final ProduitRepository produitRepository;
    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final PdfGenerationService pdfGenerationService;
    private final TemplateEngine templateEngine;
    private final ParametreService parametreService;

    public RapportController(ProduitRepository produitRepository, VenteRepository venteRepository, LigneVenteRepository ligneVenteRepository, PdfGenerationService pdfGenerationService, TemplateEngine templateEngine, ParametreService parametreService) {
        this.produitRepository = produitRepository;
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.templateEngine = templateEngine;
        this.parametreService = parametreService;
    }

    @GetMapping("/stock-bas")
    public String rapportStockBas(Model model,
                                  @RequestParam(defaultValue = "stock") String tab,
                                  @RequestParam(required = false) String filter,
                                  @RequestParam(defaultValue = "asc") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "150") int size,
                                  @RequestParam(required = false) String searchTerm) {

        model.addAttribute("activeTab", tab);

        int seuilStockBasInt = parametreService.getSeuilStockBas();
        BigDecimal seuilStockBas = BigDecimal.valueOf(seuilStockBasInt);
        int joursAvantPeremption = parametreService.getJoursAvantPeremption();

        Sort.Direction direction = "desc".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "quantiteEnStock"));

        Page<Produit> produitsPage;
        if (searchTerm != null && !searchTerm.isEmpty()) {
            if ("rupture".equals(filter)) {
                produitsPage = produitRepository.findByNomContainingIgnoreCaseAndQuantiteEnStock(searchTerm, BigDecimal.ZERO, pageable);
            } else {
                produitsPage = produitRepository.findByNomContainingIgnoreCaseAndQuantiteEnStockLessThanEqual(searchTerm, seuilStockBas, pageable);
            }
        } else {
            if ("rupture".equals(filter)) {
                produitsPage = produitRepository.findAllByQuantiteEnStock(BigDecimal.ZERO, pageable);
            } else {
                produitsPage = produitRepository.findAllByQuantiteEnStockLessThanEqual(seuilStockBas, pageable);
            }
        }

        Pageable top10 = PageRequest.of(0, 10);
        List<Produit> produitsPourStockChart = produitRepository.findTopNByQuantiteEnStockLessThanEqualOrderByQuantiteEnStockAsc(seuilStockBas, top10);

        List<Produit> tousLesProduitsEnStockBas = produitRepository.findAllByQuantiteEnStockLessThanEqual(seuilStockBas, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        long nombreEnRupture = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getQuantiteEnStock().compareTo(BigDecimal.ZERO) == 0)
                .count();

        BigDecimal valeurStockBas = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getPrixAchat() != null)
                .map(p -> p.getPrixAchat().multiply(p.getQuantiteEnStock()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateLimite = aujourdhui.plusDays(joursAvantPeremption);
        List<Produit> produitsPeremptionProche = produitRepository.findAllByDatePeremptionBetweenAndQuantiteEnStockGreaterThan(aujourdhui, dateLimite, BigDecimal.ZERO);

        // Force initialization to prevent LazyInitializationException in the template
        List<Produit> initializedProduits = produitsPage.getContent();
        // Simply accessing the list might be enough if the transaction is managed correctly up to the view layer,
        // but creating a new Page object from a collected list is safer.
        Page<Produit> initializedPage = new org.springframework.data.domain.PageImpl<>(
                initializedProduits.stream().collect(Collectors.toList()),
                pageable,
                produitsPage.getTotalElements()
        );
        model.addAttribute("produitsPage", initializedPage);
        model.addAttribute("seuil", seuilStockBasInt);
        model.addAttribute("sort", sort);
        model.addAttribute("activeFilter", filter);
        model.addAttribute("nombreStockBas", tousLesProduitsEnStockBas.size());
        model.addAttribute("nombreEnRupture", nombreEnRupture);
        model.addAttribute("valeurStockBas", valeurStockBas);
        model.addAttribute("produitsPeremptionProche", produitsPeremptionProche);
        model.addAttribute("joursAvantPeremption", joursAvantPeremption);
        model.addAttribute("totalProduits", produitRepository.count());
        model.addAttribute("produitsPourStockChart", produitsPourStockChart);
        model.addAttribute("searchTerm", searchTerm);

        return "rapport-stock-bas";
    }

    @GetMapping("/ventes-historique")
    public String ventesHistorique(Model model,
                                   @RequestParam(defaultValue = "historique") String tab,
                                   @RequestParam(required = false) String nomProduit,
                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   @RequestParam(required = false) Double minAmount,
                                   @RequestParam(required = false) Double maxAmount,
                                   @RequestParam(defaultValue = "date") String sortBy,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "50") int size) {

        model.addAttribute("activeTab", tab);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("minAmount", minAmount);
        model.addAttribute("maxAmount", maxAmount);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("size", size);

        Sort sort = Sort.by(Sort.Direction.DESC, "dateVente");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<com.example.boutique.model.Vente> ventePage;

        if (startDate != null && endDate != null) {
            ventePage = venteRepository.findByDateVenteBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable);
        } else {
            ventePage = venteRepository.findAll(pageable);
        }

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
        model.addAttribute("revenueChange", 0.0);
        model.addAttribute("salesChange", 0.0);
        model.addAttribute("todayRevenueChange", 0.0);

        List<Object[]> mostSoldProductsRaw = ligneVenteRepository.findMostSoldProducts();
        BigDecimal totalItemsSold = mostSoldProductsRaw.stream()
                .map(item -> (BigDecimal) item[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> mostSoldProducts = mostSoldProductsRaw.stream()
                .map(item -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("produit", item[0]);
                    BigDecimal quantite = (BigDecimal) item[1];
                    productMap.put("quantite", quantite);

                    if (totalItemsSold.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal percentage = quantite.multiply(new BigDecimal("100")).divide(totalItemsSold, 2, java.math.RoundingMode.HALF_UP);
                        productMap.put("percentage", percentage);
                    } else {
                        productMap.put("percentage", BigDecimal.ZERO);
                    }
                    return productMap;
                })
                .collect(Collectors.toList());
        model.addAttribute("mostSoldProducts", mostSoldProducts);

        // Add stats for the new "Sales by Product" tab, applying filters
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;
        List<com.example.boutique.dto.ProduitVenteStatsDto> produitVenteStats = ligneVenteRepository.findProduitVenteStats(nomProduit, startDateTime, endDateTime);
        model.addAttribute("produitVenteStats", produitVenteStats);

        return "ventes-historique";
    }

    @GetMapping("/ventes/imprimer")
    public String imprimerVentes(Model model,
                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                 @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");
        List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort).stream().filter(v -> v.getStatus() != com.example.boutique.enums.VenteStatus.CANCELLED).collect(Collectors.toList());

        List<LigneVente> allLigneVentes = ventes.stream()
                .flatMap(vente -> vente.getLigneVentes().stream())
                .collect(Collectors.toList());

        BigDecimal totalGeneral = allLigneVentes.stream()
                .map(LigneVente::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("allLigneVentes", allLigneVentes);
        model.addAttribute("totalGeneral", totalGeneral);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
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

    @GetMapping("/ventes/export/excel")
    public void exportVentesRapportExcel(HttpServletResponse response,
                                         @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

            Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");
            List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort).stream().filter(v -> v.getStatus() != com.example.boutique.enums.VenteStatus.CANCELLED).collect(Collectors.toList());

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=rapport_ventes.xlsx");

            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Rapport des Ventes");

                Row headerRow = sheet.createRow(0);
                String[] headers = {"Date", "Nom Produit", "Quantité", "Prix Unitaire", "Total Ligne"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                }

                int rowNum = 1;
                for (com.example.boutique.model.Vente vente : ventes) {
                    for (LigneVente ligne : vente.getLigneVentes()) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(vente.getDateVente().toString());
                        row.createCell(1).setCellValue(ligne.getProduit().getNom());
                        row.createCell(2).setCellValue(ligne.getQuantite().doubleValue());
                        row.createCell(3).setCellValue(ligne.getPrixUnitaire().doubleValue());
                        row.createCell(4).setCellValue(ligne.getMontantTotal().doubleValue());
                    }
                }
                workbook.write(response.getOutputStream());
            }
        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport Excel des ventes.", e);
        }
    }

    @GetMapping("/ventes/export/pdf")
    public ResponseEntity<byte[]> exportRapportVentesPdf(@RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                     @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

            Sort sort = Sort.by(Sort.Direction.ASC, "dateVente");
            List<com.example.boutique.model.Vente> ventes = venteRepository.findAllWithDetailsByDateVenteBetween(startDateTime, endDateTime, sort).stream().filter(v -> v.getStatus() != com.example.boutique.enums.VenteStatus.CANCELLED).collect(Collectors.toList());

            List<LigneVente> allLigneVentes = ventes.stream()
                    .flatMap(vente -> vente.getLigneVentes().stream())
                    .collect(Collectors.toList());

            BigDecimal totalGeneral = allLigneVentes.stream()
                    .map(LigneVente::getMontantTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> data = new HashMap<>();
            data.put("allLigneVentes", allLigneVentes);
            data.put("totalGeneral", totalGeneral);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            data.put("dateGeneration", LocalDateTime.now());

            byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("rapport-ventes", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rapport-ventes.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport PDF des ventes.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/imprimer/stock-bas")
    public ResponseEntity<byte[]> imprimerRapportStockBas(
            @RequestParam(required = false) String filter) {
        try {
            int seuilStockBasInt = parametreService.getSeuilStockBas();
            BigDecimal seuilStockBas = BigDecimal.valueOf(seuilStockBasInt);

            List<Produit> produits;
            String typeRapport;
            if ("rupture".equals(filter)) {
                produits = produitRepository.findAllByQuantiteEnStock(BigDecimal.ZERO, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
                typeRapport = "Produits en Rupture de Stock";
            } else {
                produits = produitRepository.findAllByQuantiteEnStockLessThanEqual(seuilStockBas, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
                typeRapport = "Produits en Stock Bas";
            }

            Map<String, Object> data = new HashMap<>();
            data.put("produits", produits);
            data.put("dateGeneration", LocalDateTime.now());
            data.put("typeRapport", typeRapport);

            byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("rapport-stock-bas-print", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rapport-stock-bas.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport PDF de stock bas.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/imprimer/peremption")
    public ResponseEntity<byte[]> imprimerRapportPeremption(Model model) {
        try {
            int joursAvantPeremption = parametreService.getJoursAvantPeremption();
            LocalDate aujourdhui = LocalDate.now();
            LocalDate dateLimite = aujourdhui.plusDays(joursAvantPeremption);
            List<Produit> produitsPeremptionProche = produitRepository.findAllByDatePeremptionBetweenAndQuantiteEnStockGreaterThan(aujourdhui, dateLimite, BigDecimal.ZERO);

            Map<String, Object> data = new HashMap<>();
            data.put("produits", produitsPeremptionProche);
            data.put("dateGeneration", LocalDateTime.now());
            data.put("joursAvantPeremption", joursAvantPeremption);
            data.put("typeRapport", "Produits avec date de péremption proche");

            byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("rapport-peremption-print", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rapport-peremption.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport PDF de péremption.", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/produits/export/excel")
    public void exportVentesParProduitExcel(HttpServletResponse response,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
                                     @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.plusDays(1).atStartOfDay() : null;

        List<com.example.boutique.dto.ProduitVenteStatsDto> stats = ligneVenteRepository.findProduitVenteStats(keyword, startDateTime, endDateTime);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=rapport_ventes_par_produit.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Ventes par Produit");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Produit", "Quantité Vendue", "Prix Vente Actuel", "Revenu Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (com.example.boutique.dto.ProduitVenteStatsDto stat : stats) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stat.getProduit().getNom());
                row.createCell(1).setCellValue(stat.getTotalQuantiteVendue().doubleValue());
                if (stat.getProduit().getPrixVenteUnitaire() != null) {
                    row.createCell(2).setCellValue(stat.getProduit().getPrixVenteUnitaire().doubleValue());
                }
                row.createCell(3).setCellValue(stat.getTotalRevenu().doubleValue());
            }
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/produits/export/pdf")
    public ResponseEntity<byte[]> exportVentesParProduitPdf(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDate.now();
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

            List<com.example.boutique.dto.ProduitVenteStatsDto> stats = ligneVenteRepository.findProduitVenteStats(keyword, startDateTime, endDateTime);

            Map<String, Object> data = new HashMap<>();
            data.put("produitVenteStats", stats);
            data.put("startDate", startDate);
            data.put("endDate", endDate);
            data.put("dateGeneration", LocalDateTime.now());

            byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("rapport-ventes-par-produit-print", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "rapport_ventes_par_produit.pdf");

            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (IOException e) {
            logger.error("Erreur lors de la génération du rapport PDF des ventes par produit.", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}