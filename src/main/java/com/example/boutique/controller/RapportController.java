package com.example.boutique.controller;

import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.LigneVenteRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.VenteRepository;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private static final int SEUIL_STOCK_BAS = 10;
    private static final int JOURS_AVANT_PEREMPTION = 30;

    public RapportController(ProduitRepository produitRepository, VenteRepository venteRepository, LigneVenteRepository ligneVenteRepository) {
        this.produitRepository = produitRepository;
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
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
                                   @RequestParam(required = false) BigDecimal minAmount,
                                   @RequestParam(required = false) BigDecimal maxAmount,
                                   @RequestParam(defaultValue = "date") String sortBy,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {

        Sort sort = switch (sortBy) {
            case "date_asc" -> Sort.by(Sort.Direction.ASC, "vente.dateVente");
            case "amount" -> Sort.by(Sort.Direction.DESC, "montantTotal");
            case "amount_asc" -> Sort.by(Sort.Direction.ASC, "montantTotal");
            case "product" -> Sort.by(Sort.Direction.ASC, "produit.nom");
            default -> Sort.by(Sort.Direction.DESC, "vente.dateVente");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LigneVente> ligneVentePage;

        if (nomProduit != null && !nomProduit.isEmpty()) {
            ligneVentePage = ligneVenteRepository.findByProduitNomContainingIgnoreCase(nomProduit, pageable);
        } else if (startDate != null && endDate != null) {
            ligneVentePage = ligneVenteRepository.findByVenteDateVenteBetween(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable);
        } else {
            ligneVentePage = ligneVenteRepository.findAll(pageable);
        }

        List<LigneVente> ligneVentes = ligneVentePage.getContent().stream()
                .filter(lv -> (minAmount == null || lv.getMontantTotal().compareTo(minAmount) >= 0))
                .filter(lv -> (maxAmount == null || lv.getMontantTotal().compareTo(maxAmount) <= 0))
                .collect(Collectors.toList());

        // Calculate metrics
        BigDecimal totalRevenue = ligneVentes.stream()
                .map(LigneVente::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalSales = ligneVentes.stream()
                .map(LigneVente::getVente)
                .distinct()
                .count();

        BigDecimal todaysRevenue = ligneVentes.stream()
                .filter(lv -> lv.getVente().getDateVente().toLocalDate().isEqual(LocalDate.now()))
                .map(LigneVente::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Previous period metrics for trends
        LocalDateTime lastMonthStart = LocalDate.now().minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<LigneVente> lastMonthVentes = ligneVenteRepository.findByVenteDateVenteBetween(lastMonthStart, lastMonthEnd, Pageable.unpaged()).getContent();
        BigDecimal previousMonthRevenue = lastMonthVentes.stream()
            .map(LigneVente::getMontantTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long previousMonthSales = lastMonthVentes.stream()
            .map(LigneVente::getVente)
            .distinct()
            .count();

        LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = LocalDate.now().atStartOfDay();
        List<LigneVente> yesterdayVentes = ligneVenteRepository.findByVenteDateVenteBetween(yesterdayStart, yesterdayEnd, Pageable.unpaged()).getContent();
        BigDecimal yesterdayRevenue = yesterdayVentes.stream()
            .map(LigneVente::getMontantTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate percentage changes
        double revenueChange = 0;
        if (previousMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            revenueChange = (totalRevenue.subtract(previousMonthRevenue)).divide(previousMonthRevenue, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
        }
        double salesChange = 0;
        if (previousMonthSales > 0) {
            salesChange = ((double) totalSales - previousMonthSales) / previousMonthSales * 100;
        }
        double todayRevenueChange = 0;
        if (yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            todayRevenueChange = (todaysRevenue.subtract(yesterdayRevenue)).divide(yesterdayRevenue, 2, BigDecimal.ROUND_HALF_UP).doubleValue() * 100;
        }

        model.addAttribute("ventesPage", new PageImpl<>(ligneVentes, pageable, ligneVentePage.getTotalElements()));
        model.addAttribute("produits", produitRepository.findAll(Sort.by("nom")));
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalSales", totalSales);
        model.addAttribute("todaysRevenue", todaysRevenue);
        model.addAttribute("revenueChange", revenueChange);
        model.addAttribute("salesChange", salesChange);
        model.addAttribute("todayRevenueChange", todayRevenueChange);

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
            List<LigneVente> sales = ligneVenteRepository.findAll();
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

    @GetMapping("/export/excel")
    @ResponseBody
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; file=ventes.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Ventes");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] header = {"ID Vente", "Produit", "Quantité", "Prix Unitaire", "Montant Total", "Date de Vente"};
            for (int i = 0; i < header.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(header[i]);
            }

            // Create data rows
            List<LigneVente> sales = ligneVenteRepository.findAll();
            int rowNum = 1;
            for (LigneVente sale : sales) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sale.getVente().getId());
                row.createCell(1).setCellValue(sale.getProduit().getNom());
                row.createCell(2).setCellValue(sale.getQuantite());
                row.createCell(3).setCellValue(sale.getPrixUnitaire().doubleValue());
                row.createCell(4).setCellValue(sale.getMontantTotal().doubleValue());
                row.createCell(5).setCellValue(sale.getVente().getDateVente().toString());
            }

            workbook.write(response.getOutputStream());
        }
    }
}