package com.example.boutique.controller;

import com.example.boutique.dto.CategoryProductCount;
import com.example.boutique.dto.CategorySales;
import com.example.boutique.dto.MouvementStatDto;
import com.example.boutique.dto.ProduitVenteDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.repository.*;
import com.example.boutique.service.ParametreService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final ProduitRepository produitRepository;
    private final PersonnelRepository personnelRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MouvementStockRepository mouvementStockRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final VenteRepository venteRepository;
    private final ParametreService parametreService;


    public DashboardController(ProduitRepository produitRepository, PersonnelRepository personnelRepository, UtilisateurRepository utilisateurRepository, MouvementStockRepository mouvementStockRepository, LigneVenteRepository ligneVenteRepository, VenteRepository venteRepository, ParametreService parametreService) {
        this.produitRepository = produitRepository;
        this.personnelRepository = personnelRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.mouvementStockRepository = mouvementStockRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.venteRepository = venteRepository;
        this.parametreService = parametreService;
    }

    @GetMapping
    public String showDashboard(Model model) {
        try {
            addKpiData(model);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des indicateurs clés (KPIs).", e);
            model.addAttribute("kpiError", "Impossible de charger les indicateurs clés.");
        }

        try {
            model.addAttribute("derniersMouvements", mouvementStockRepository.findTop5ByOrderByDateMouvementDesc());
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des derniers mouvements.", e);
            model.addAttribute("mouvementsError", "Impossible de charger les mouvements récents.");
        }

        try {
            addChartData(model);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des données du graphique d'activité.", e);
            model.addAttribute("chartDataError", "Impossible de charger le graphique d'activité.");
        }

        try {
            addTopSellingProducts(model);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement des produits les plus vendus.", e);
            model.addAttribute("topProductsError", "Impossible de charger les produits populaires.");
        }

        try {
            addCategoryChartData(model);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement du graphique des catégories.", e);
            model.addAttribute("categoryChartError", "Impossible de charger le graphique des catégories.");
        }

        try {
            addSalesByCategoryChartData(model);
        } catch (Exception e) {
            logger.error("Erreur lors du chargement du graphique des ventes par catégorie.", e);
            model.addAttribute("salesByCategoryError", "Impossible de charger les ventes par catégorie.");
        }

        return "dashboard";
    }

    @GetMapping("/api/ventes-par-jour")
    @ResponseBody
    public Map<String, Object> getVentesParJour(@RequestParam(defaultValue = "7") int jours) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(jours).with(LocalTime.MIN);

        List<Object[]> salesData = venteRepository.findSalesPerDay(startDate, endDate);

        // Create a map of dates to sales totals
        Map<LocalDate, BigDecimal> salesByDate = salesData.stream()
            .collect(Collectors.toMap(
                row -> ((java.sql.Date) row[0]).toLocalDate(),
                row -> (BigDecimal) row[1]
            ));

        // Prepare labels and data for the last N days, filling in zeros for days with no sales
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();

        for (int i = 0; i < jours; i++) {
            LocalDate date = endDate.minusDays(i).toLocalDate();
            labels.add(date.format(DateTimeFormatter.ofPattern("dd/MM")));
            data.add(salesByDate.getOrDefault(date, BigDecimal.ZERO));
        }

        // Reverse the lists to have the oldest date first
        java.util.Collections.reverse(labels);
        java.util.Collections.reverse(data);

        Map<String, Object> response = new HashMap<>();
        response.put("labels", labels);
        response.put("data", data);

        return response;
    }

    private void addKpiData(Model model) {
        // KPIs existants
        int seuilStockBas = parametreService.getSeuilStockBas();
        model.addAttribute("totalProduits", produitRepository.count());
        model.addAttribute("produitsStockBas", produitRepository.countByQuantiteEnStockLessThanEqual(BigDecimal.valueOf(seuilStockBas)));
        model.addAttribute("totalPersonnel", personnelRepository.count());
        model.addAttribute("totalUtilisateurs", utilisateurRepository.count());

        // 1. Valeur totale du stock
        BigDecimal valeurStock = produitRepository.findAll().stream()
                .map(p -> p.getQuantiteEnStock().multiply(p.getPrixVenteUnitaire()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("valeurStock", valeurStock);

        // 2. Indicateurs de ventes
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        // Ventes du jour
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        List<MouvementStock> ventesAujourdhui = mouvementStockRepository.findByTypeMouvementAndDateMouvementBetween(TypeMouvement.SORTIE_VENTE, startOfDay, endOfDay);
        BigDecimal chiffreAffairesAujourdhui = ventesAujourdhui.stream()
                .map(mvt -> mvt.getQuantite().multiply(mvt.getProduit().getApplicablePrix()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("chiffreAffairesAujourdhui", chiffreAffairesAujourdhui);
        model.addAttribute("nombreVentesAujourdhui", (long) ventesAujourdhui.size());

        // Ventes du mois
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<MouvementStock> ventesMois = mouvementStockRepository.findByTypeMouvementAndDateMouvementBetween(TypeMouvement.SORTIE_VENTE, startOfMonth, endOfMonth);
        BigDecimal chiffreAffairesMois = ventesMois.stream()
                .map(mvt -> mvt.getQuantite().multiply(mvt.getProduit().getApplicablePrix()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("chiffreAffairesMois", chiffreAffairesMois);
    }

    private void addChartData(Model model) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(6);

        // 4. Données pour le graphique Entrées/Sorties
        List<MouvementStatDto> entreesStats = mouvementStockRepository.countMouvementsByDayAndType(startDate, TypeMouvement.ENTREE);
        List<MouvementStatDto> sortiesStats = mouvementStockRepository.countMouvementsByDayAndType(startDate, TypeMouvement.SORTIE_VENTE);

        Map<String, Long> entreesMap = entreesStats.stream()
                .collect(Collectors.toMap(s -> s.getDate().format(DateTimeFormatter.ofPattern("dd/MM")), MouvementStatDto::getCount));
        Map<String, Long> sortiesMap = sortiesStats.stream()
                .collect(Collectors.toMap(s -> s.getDate().format(DateTimeFormatter.ofPattern("dd/MM")), MouvementStatDto::getCount));

        List<String> chartLabels = IntStream.range(0, 7)
                .mapToObj(i -> endDate.minusDays(i).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM")))
                .sorted()
                .collect(Collectors.toList());

        List<Long> entreesData = chartLabels.stream()
                .map(label -> entreesMap.getOrDefault(label, 0L))
                .collect(Collectors.toList());

        List<Long> sortiesData = chartLabels.stream()
                .map(label -> sortiesMap.getOrDefault(label, 0L))
                .collect(Collectors.toList());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("entreesData", entreesData);
        model.addAttribute("sortiesData", sortiesData);
    }

    private void addTopSellingProducts(Model model) {
        // 3. Produits les plus vendus
        List<ProduitVenteDto> topProduits = mouvementStockRepository.findTopSellingProducts(PageRequest.of(0, 5));
        model.addAttribute("topProduits", topProduits);
    }

    private void addCategoryChartData(Model model) {
        List<CategoryProductCount> categoryCounts = produitRepository.countProductsByCategory();
        List<String> categoryLabels = categoryCounts.stream().map(CategoryProductCount::getCategory).collect(Collectors.toList());
        List<Long> categoryData = categoryCounts.stream().map(CategoryProductCount::getProductCount).collect(Collectors.toList());
        model.addAttribute("categoryLabels", categoryLabels);
        model.addAttribute("categoryData", categoryData);
    }

    private void addSalesByCategoryChartData(Model model) {
        List<CategorySales> salesByCategory = ligneVenteRepository.findTotalSalesByCategory();
        List<String> salesByCategoryLabels = salesByCategory.stream().map(CategorySales::getCategory).collect(Collectors.toList());
        List<BigDecimal> salesByCategoryData = salesByCategory.stream().map(CategorySales::getTotalSales).collect(Collectors.toList());
        model.addAttribute("salesByCategoryLabels", salesByCategoryLabels);
        model.addAttribute("salesByCategoryData", salesByCategoryData);
    }
}
