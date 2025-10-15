package com.example.boutique.controller;

import com.example.boutique.dto.MouvementStatDto;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.PersonnelRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final ProduitRepository produitRepository;
    private final PersonnelRepository personnelRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final MouvementStockRepository mouvementStockRepository;

    private static final int SEUIL_STOCK_BAS = 10;

    public DashboardController(ProduitRepository produitRepository, PersonnelRepository personnelRepository, UtilisateurRepository utilisateurRepository, MouvementStockRepository mouvementStockRepository) {
        this.produitRepository = produitRepository;
        this.personnelRepository = personnelRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.mouvementStockRepository = mouvementStockRepository;
    }

    @GetMapping
    public String showDashboard(Model model) {
        // KPI Cards data
        model.addAttribute("totalProduits", produitRepository.count());
        model.addAttribute("produitsStockBas", produitRepository.countByQuantiteEnStockLessThanEqual(SEUIL_STOCK_BAS));
        model.addAttribute("totalPersonnel", personnelRepository.count());
        model.addAttribute("totalUtilisateurs", utilisateurRepository.count());

        // Recent movements list data
        model.addAttribute("derniersMouvements", mouvementStockRepository.findTop5ByOrderByDateMouvementDesc());

        // Chart data
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(6);

        List<MouvementStatDto> stats = mouvementStockRepository.countMouvementsByDay(startDate);
        Map<String, Long> statsMap = stats.stream()
                .collect(Collectors.toMap(s -> s.getDate().format(DateTimeFormatter.ofPattern("dd/MM")), MouvementStatDto::getCount));

        List<String> chartLabels = IntStream.range(0, 7)
                .mapToObj(i -> endDate.minusDays(i).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM")))
                .sorted()
                .collect(Collectors.toList());

        List<Long> chartData = chartLabels.stream()
                .map(label -> statsMap.getOrDefault(label, 0L))
                .collect(Collectors.toList());

        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);

        return "dashboard";
    }
}
