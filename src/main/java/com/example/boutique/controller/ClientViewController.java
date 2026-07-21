package com.example.boutique.controller;

import com.example.boutique.model.Client;
import com.example.boutique.dto.VenteCreditDto;
import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.VenteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/clients")
public class ClientViewController {

    private static final Logger logger = LoggerFactory.getLogger(ClientViewController.class);

    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;

    public ClientViewController(ClientRepository clientRepository, VenteRepository venteRepository) {
        this.clientRepository = clientRepository;
        this.venteRepository = venteRepository;
    }

    @GetMapping
    public String listClients(Model model,
                              @RequestParam(defaultValue = "") String keyword,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(defaultValue = "nom") String sortField,
                              @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortField));
        Page<Client> clientPage;
        if (keyword != null && !keyword.isEmpty()) {
            clientPage = clientRepository.findByNomContainingIgnoreCase(keyword, pageable);
        } else {
            clientPage = clientRepository.findAll(pageable);
        }

        long totalClients = clientRepository.count();

        model.addAttribute("clientPage", clientPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        model.addAttribute("totalClients", totalClients);

        return "client-list";
    }

    @GetMapping("/form")
    public String showClientForm(@RequestParam(required = false) Long id, Model model) {
        Client client = id != null ? clientRepository.findById(id).orElse(new Client()) : new Client();
        model.addAttribute("client", client);
        return "fragments/client-form :: client-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveClient(@ModelAttribute Client client) {
        try {
            Client savedClient = clientRepository.save(client);
            String message = (client.getId() == null) ? "Client ajouté avec succès." : "Client modifié avec succès.";
            return ResponseEntity.ok(Map.of("success", true, "message", message));
        } catch (DataIntegrityViolationException e) {
            logger.warn("Tentative de sauvegarde d'un client avec un nom ou téléphone existant: {}", client.getNom(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Un client avec ce nom ou ce téléphone existe déjà."));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'enregistrement du client.", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur technique est survenue lors de l'enregistrement."));
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clientRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Client supprimé avec succès !");
        } catch (DataIntegrityViolationException e) {
            logger.warn("Tentative de suppression d'un client lié à des ventes. ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Impossible de supprimer ce client car il est associé à des ventes.");
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la suppression du client. ID: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Une erreur technique est survenue.");
        }
        return "redirect:/clients";
    }


    @GetMapping("/{id}/credits")
    public String getClientCreditSales(@PathVariable Long id, Model model) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client non trouvé avec l'id : " + id));
        List<VenteCreditDto> creditSales = venteRepository.findCreditSalesByClientId(id);

        BigDecimal totalCreditSales = creditSales.stream()
                .map(VenteCreditDto::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("client", client);
        model.addAttribute("creditSales", creditSales);
        model.addAttribute("totalCreditSales", totalCreditSales);
        return "client-credit-sales";
    }
}
