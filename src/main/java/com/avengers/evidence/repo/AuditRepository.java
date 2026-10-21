package com.avengers.evidence.repo;
import com.avengers.evidence.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AuditRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByOrderByIdDesc();
    List<AuditLog> findAllByOrderByIdAsc();
    List<AuditLog> findTop50ByOrderByIdDesc();
    List<AuditLog> findByEvidenceIdOrderByIdDesc(Long evidenceId);
}
