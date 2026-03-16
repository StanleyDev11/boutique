package com.example.boutique.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/system")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemStatusController {

    private final MetricsEndpoint metricsEndpoint;
    private final HealthEndpoint healthEndpoint;

    @GetMapping("/status")
    public String getSystemStatus(Model model) {
        model.addAttribute("health", healthEndpoint.health());
        
        // On passe les noms des métriques pour que le JS puisse les interroger si besoin
        // ou on envoie des valeurs initiales
        model.addAttribute("jvmMemoryUsed", getMetricValue("jvm.memory.used"));
        model.addAttribute("jvmMemoryMax", getMetricValue("jvm.memory.max"));
        model.addAttribute("cpuUsage", getMetricValue("system.cpu.usage"));
        
        return "system-status";
    }

    private Double getMetricValue(String metricName) {
        try {
            MetricsEndpoint.MetricDescriptor response = metricsEndpoint.metric(metricName, java.util.Collections.emptyList());
            if (response != null && !response.getMeasurements().isEmpty()) {
                return response.getMeasurements().get(0).getValue();
            }
        } catch (Exception e) {
            return 0.0;
        }
        return 0.0;
    }
}
