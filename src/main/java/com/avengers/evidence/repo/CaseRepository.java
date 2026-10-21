package com.avengers.evidence.repo;
import com.avengers.evidence.model.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CaseRepository extends JpaRepository<CaseFile, Long> { }
