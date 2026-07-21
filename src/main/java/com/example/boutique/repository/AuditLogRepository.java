package com.example.boutique.repository;

import com.example.boutique.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByType(String type, Pageable pageable);
    Page<AuditLog> findByUtilisateur(String utilisateur, Pageable pageable);
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
