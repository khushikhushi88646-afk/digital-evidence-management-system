package com.avengers.evidence.controller;

import com.avengers.evidence.model.CaseFile;
import com.avengers.evidence.repo.CaseRepository;
import com.avengers.evidence.service.AuditService;
import com.avengers.evidence.service.CacheService;
import com.avengers.evidence.service.Timed;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cases")
public class CaseController {
    private final CaseRepository repo; private final CacheService cache; private final AuditService audit;

    public CaseController(CaseRepository repo, CacheService cache, AuditService audit) {
        this.repo = repo; this.cache = cache; this.audit = audit;
    }

    @GetMapping
    public Timed<List<CaseFile>> list() {
        long t0 = System.nanoTime();
        Optional<List<CaseFile>> hit = cache.get("cases:all", cache.listOf(CaseFile.class));
        if (hit.isPresent()) return new Timed<>(hit.get(), "REDIS", (System.nanoTime() - t0) / 1e6);
        List<CaseFile> l = repo.findAll();
        cache.put("cases:all", l);
        return new Timed<>(l, "DATABASE", (System.nanoTime() - t0) / 1e6);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CaseFile c, Authentication auth) {
        if (c.getTitle() == null || c.getTitle().isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Title required"));
        c.setCaseNumber("CASE-" + java.time.Year.now() + "-" + String.format("%03d", repo.count() + 1));
        c.setCreatedBy(auth.getName());
        c.setStatus("OPEN");
        CaseFile saved = repo.save(c);
        cache.evict("cases:all");
        audit.log(auth.getName(), "CASE_CREATED", null, saved.getCaseNumber() + " " + saved.getTitle());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> status(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication auth) {
        return repo.findById(id).map(c -> {
            c.setStatus(b.getOrDefault("status", "OPEN"));
            repo.save(c);
            cache.evict("cases:all");
            audit.log(auth.getName(), "CASE_STATUS", null, c.getCaseNumber() + " -> " + c.getStatus());
            return ResponseEntity.ok((Object) c);
        }).orElse(ResponseEntity.notFound().build());
    }
}
