package com.example.boutique.controller;

import com.example.boutique.repository.PlanRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/tarifs")
public class TarifsController {

    private final PlanRepository planRepository;

    public TarifsController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public String showTarifsPage(Model model) {
        model.addAttribute("plans", planRepository.findAll());
        return "tarifs";
    }
}
