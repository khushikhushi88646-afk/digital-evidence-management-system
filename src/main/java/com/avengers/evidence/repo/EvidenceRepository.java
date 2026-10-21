package com.avengers.evidence.repo;
import com.avengers.evidence.model.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByCaseIdOrderByUploadedAtDesc(Long caseId);
    List<Evidence> findByTitleContainingIgnoreCaseOrOriginalFileNameContainingIgnoreCaseOrSha256Containing(String t, String f, String h);
    long countByIntegrityStatus(String status);
}
