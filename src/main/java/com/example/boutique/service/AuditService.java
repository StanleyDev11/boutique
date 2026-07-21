package com.example.boutique.service;

import com.example.boutique.model.AuditLog;
import com.example.boutique.repository.AuditLogRepository;
import com.example.boutique.utils.UserUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void log(String action, String detail, String entite, String entiteId, String type) {
        String utilisateur = UserUtils.getCurrentUsername();
        if (utilisateur == null) {
            utilisateur = "SYSTEM";
        }

        String ip = getClientIp();

        AuditLog log = AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .utilisateur(utilisateur)
                .action(action)
                .detail(detail)
                .entite(entite)
                .entiteId(entiteId)
                .adresseIp(ip)
                .type(type)
                .build();

        auditLogRepository.save(log);
    }

    public void logInfo(String action, String detail) {
        log(action, detail, null, null, "INFO");
    }

    public void logWarning(String action, String detail) {
        log(action, detail, null, null, "WARNING");
    }

    public void logError(String action, String detail) {
        log(action, detail, null, null, "ERROR");
    }

    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0];
        }
        return "UNKNOWN";
    }
}
