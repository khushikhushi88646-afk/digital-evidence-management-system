package com.avengers.evidence.controller;

import com.avengers.evidence.model.Evidence;
import com.avengers.evidence.service.EvidenceService;
import com.avengers.evidence.service.Timed;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {
    private final EvidenceService svc;
    public EvidenceController(EvidenceService svc) { this.svc = svc; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Evidence upload(@RequestParam MultipartFile file, @RequestParam Long caseId,
                           @RequestParam(required = false) String title, @RequestParam(required = false) String description,
                           @RequestParam(required = false) String type, Authentication auth) throws IOException {
        return svc.upload(file, caseId, title, description, type, auth.getName());
    }

    @GetMapping
    public Timed<List<Evidence>> all(@RequestParam(required = false) Long caseId, @RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) return svc.search(q);
        if (caseId != null) return svc.byCase(caseId);
        return svc.all();
    }

    @GetMapping("/{id}")
    public Timed<Evidence> one(@PathVariable Long id, Authentication auth) { return svc.get(id, auth.getName()); }

    @PostMapping("/{id}/verify")
    public Evidence verify(@PathVariable Long id, Authentication auth) throws IOException { return svc.verify(id, auth.getName()); }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        Evidence e = svc.meta(id);
        Resource r = svc.download(id, auth.getName());
        String ct = e.getContentType() == null ? "application/octet-stream" : e.getContentType();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(ct))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(e.getOriginalFileName()).build().toString())
                .body(r);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable Long id, Authentication auth) throws IOException {
        svc.delete(id, auth.getName());
        return Map.of("status", "deleted");
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<?> nf(NoSuchElementException e) { return ResponseEntity.status(404).body(Map.of("error", e.getMessage())); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> bad(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
}
