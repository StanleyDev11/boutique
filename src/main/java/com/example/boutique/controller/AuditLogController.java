package com.example.boutique.controller;

import com.example.boutique.model.AuditLog;
import com.example.boutique.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {
    private final AuditService auditService;

    @GetMapping
    public String listLogs(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> logs = auditService.getAllLogs(PageRequest.of(page, size));
        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        return "audit-logs";
    }
}
