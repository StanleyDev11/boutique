package com.example.boutique.controller;

import com.example.boutique.model.Client;
import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.VenteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientViewController {

    private final ClientRepository clientRepository;
    private final VenteRepository venteRepository;

    public ClientViewController(ClientRepository clientRepository, VenteRepository venteRepository) {
        this.clientRepository = clientRepository;
        this.venteRepository = venteRepository;
    }

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("clients", clientRepository.findAll());
        return "client-list";
    }

    @GetMapping("/{id}/credits")
    public String getClientCreditSales(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientRepository.findById(id).orElseThrow());
        model.addAttribute("creditSales", venteRepository.findCreditSalesByClientId(id));
        return "client-credit-sales";
    }
}
