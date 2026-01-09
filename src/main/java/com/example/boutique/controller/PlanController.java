package com.example.boutique.controller;

import com.example.boutique.model.Feature;
import com.example.boutique.model.Plan;
import com.example.boutique.repository.PlanRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@Controller
@RequestMapping("/superadmin/plans")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public String listPlans(Model model) {
        model.addAttribute("plans", planRepository.findAll());
        return "superadmin/plans/list";
    }

    @GetMapping("/new")
    public String showNewPlanForm(Model model) {
        model.addAttribute("plan", new Plan());
        model.addAttribute("allFeatures", Feature.values());
        return "superadmin/plans/form";
    }

    @PostMapping("/save")
    public String savePlan(@ModelAttribute Plan plan) {
        planRepository.save(plan);
        return "redirect:/superadmin/plans";
    }

    @GetMapping("/edit/{id}")
    public String showEditPlanForm(@PathVariable Long id, Model model) {
        Optional<Plan> plan = planRepository.findById(id);
        if (plan.isPresent()) {
            model.addAttribute("plan", plan.get());
            model.addAttribute("allFeatures", Feature.values());
            return "superadmin/plans/form";
        }
        return "redirect:/superadmin/plans";
    }

    @PostMapping("/update/{id}")
    public String updatePlan(@PathVariable Long id, @ModelAttribute Plan plan) {
        plan.setId(id);
        planRepository.save(plan);
        return "redirect:/superadmin/plans";
    }

    @GetMapping("/delete/{id}")
    public String deletePlan(@PathVariable Long id) {
        planRepository.deleteById(id);
        return "redirect:/superadmin/plans";
    }
}
