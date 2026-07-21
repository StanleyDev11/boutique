package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String utilisateur;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private String entite;
    
    private String entiteId;

    private String adresseIp;

    @Column(nullable = false)
    private String type; // INFO, WARNING, ERROR
}
