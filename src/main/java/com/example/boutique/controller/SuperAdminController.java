package com.example.boutique.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @GetMapping
    public String showSuperAdminDashboard() {
        return "superadmin/dashboard";
    }
}
