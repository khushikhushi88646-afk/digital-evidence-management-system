package com.avengers.evidence.service;

import com.avengers.evidence.model.AuditLog;
import com.avengers.evidence.repo.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Chain-of-custody log. Every entry's hash includes the previous entry's hash (blockchain-style). */
@Service
public class AuditService {
    private static final String GENESIS = "0".repeat(64);
    private final AuditRepository repo;

    public AuditService(AuditRepository repo) { this.repo = repo; }

    @Transactional
    public synchronized AuditLog log(String user, String action, Long evidenceId, String details) {
        String prev = repo.findTopByOrderByIdDesc().map(AuditLog::getHash).orElse(GENESIS);
        AuditLog a = new AuditLog();
        a.setTimestamp(Instant.now().toString());
        a.setUsername(user);
        a.setAction(action);
        a.setEvidenceId(evidenceId);
        a.setDetails(details);
        a.setPrevHash(prev);
        a.setHash(compute(a));
        return repo.save(a);
    }

    private String compute(AuditLog a) {
        return Hashing.sha256(String.join("|", a.getPrevHash(), a.getTimestamp(), String.valueOf(a.getUsername()),
                a.getAction(), String.valueOf(a.getEvidenceId()), String.valueOf(a.getDetails())));
    }

    public List<AuditLog> recent() { return repo.findTop50ByOrderByIdDesc(); }
    public List<AuditLog> forEvidence(Long id) { return repo.findByEvidenceIdOrderByIdDesc(id); }

    /** Re-walk the whole chain; reports the first broken link if someone edited the DB by hand. */
    public Map<String, Object> verifyChain() {
        List<AuditLog> all = repo.findAllByOrderByIdAsc();
        String prev = GENESIS;
        Map<String, Object> res = new LinkedHashMap<>();
        for (AuditLog a : all) {
            if (!a.getPrevHash().equals(prev) || !a.getHash().equals(compute(a))) {
                res.put("valid", false); res.put("entries", all.size()); res.put("brokenAtId", a.getId());
                return res;
            }
            prev = a.getHash();
        }
        res.put("valid", true); res.put("entries", all.size()); res.put("headHash", prev);
        return res;
    }
}
