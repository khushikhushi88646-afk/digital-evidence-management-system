package com.avengers.evidence.controller;

import com.avengers.evidence.model.AuditLog;
import com.avengers.evidence.service.AuditService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
    private final AuditService audit;
    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping public List<AuditLog> recent(@RequestParam(required = false) Long evidenceId) {
        return evidenceId == null ? audit.recent() : audit.forEvidence(evidenceId);
    }
    @GetMapping("/verify-chain") public Map<String, Object> verify() { return audit.verifyChain(); }
}
