package com.avengers.evidence.service;

import com.avengers.evidence.model.Evidence;
import com.avengers.evidence.repo.EvidenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class EvidenceService {
    private final EvidenceRepository repo;
    private final CacheService cache;
    private final AuditService audit;
    private final Path dir;

    public EvidenceService(EvidenceRepository repo, CacheService cache, AuditService audit,
                           @Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.repo = repo; this.cache = cache; this.audit = audit;
        this.dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    // ------------------------------------------------------------ upload
    public Evidence upload(MultipartFile file, Long caseId, String title, String desc, String type, String user) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String stored = UUID.randomUUID() + "_" + Paths.get(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()).getFileName();
        Path target = dir.resolve(stored).normalize();
        if (!target.startsWith(dir)) throw new IllegalArgumentException("Bad file name");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        Evidence e = new Evidence();
        e.setCaseId(caseId);
        e.setTitle(title == null || title.isBlank() ? file.getOriginalFilename() : title);
        e.setDescription(desc);
        e.setEvidenceType(type == null ? "OTHER" : type);
        e.setOriginalFileName(file.getOriginalFilename());
        e.setStoredFileName(stored);
        e.setContentType(file.getContentType());
        e.setSizeBytes(file.getSize());
        e.setSha256(Hashing.sha256(Files.newInputStream(target)));
        e.setUploadedBy(user);
        e = repo.save(e);

        audit.log(user, "UPLOAD", e.getId(), "File=" + e.getOriginalFileName() + " SHA256=" + e.getSha256());
        invalidateLists(caseId);
        return e;
    }

    // ------------------------------------------------------------ reads (Redis first, DB fallback)
    public Timed<Evidence> get(Long id, String user) {
        long t0 = System.nanoTime();
        String key = "evidence:" + id;
        Optional<Evidence> hit = cache.get(key, cache.type(Evidence.class));
        Evidence e; String src;
        if (hit.isPresent()) { e = hit.get(); src = "REDIS"; }
        else {
            e = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
            cache.put(key, e); src = "DATABASE";
        }
        audit.log(user, "VIEW", id, "Served from " + src);
        return new Timed<>(e, src, ms(t0));
    }

    public Timed<List<Evidence>> byCase(Long caseId) {
        long t0 = System.nanoTime();
        String key = "case:" + caseId + ":evidence";
        Optional<List<Evidence>> hit = cache.get(key, cache.listOf(Evidence.class));
        if (hit.isPresent()) return new Timed<>(hit.get(), "REDIS", ms(t0));
        List<Evidence> list = repo.findByCaseIdOrderByUploadedAtDesc(caseId);
        cache.put(key, list);
        return new Timed<>(list, "DATABASE", ms(t0));
    }

    public Timed<List<Evidence>> search(String q) {
        long t0 = System.nanoTime();
        String key = "search:" + q.toLowerCase().trim();
        Optional<List<Evidence>> hit = cache.get(key, cache.listOf(Evidence.class));
        if (hit.isPresent()) return new Timed<>(hit.get(), "REDIS", ms(t0));
        List<Evidence> list = repo.findByTitleContainingIgnoreCaseOrOriginalFileNameContainingIgnoreCaseOrSha256Containing(q, q, q);
        cache.put(key, list);
        return new Timed<>(list, "DATABASE", ms(t0));
    }

    public Timed<List<Evidence>> all() {
        long t0 = System.nanoTime();
        Optional<List<Evidence>> hit = cache.get("evidence:all", cache.listOf(Evidence.class));
        if (hit.isPresent()) return new Timed<>(hit.get(), "REDIS", ms(t0));
        List<Evidence> list = repo.findAll();
        cache.put("evidence:all", list);
        return new Timed<>(list, "DATABASE", ms(t0));
    }

    // ------------------------------------------------------------ integrity
    public Evidence verify(Long id, String user) throws IOException {
        Evidence e = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
        Path p = dir.resolve(e.getStoredFileName()).normalize();
        String status; String now = "";
        if (!Files.exists(p)) { status = "TAMPERED"; now = "FILE MISSING"; }
        else {
            now = Hashing.sha256(Files.newInputStream(p));
            status = now.equals(e.getSha256()) ? "VERIFIED" : "TAMPERED";
        }
        e.setIntegrityStatus(status);
        e.setLastVerifiedAt(Instant.now());
        repo.save(e);
        cache.evict("evidence:" + id);
        invalidateLists(e.getCaseId());
        audit.log(user, "VERIFY", id, "Result=" + status + " stored=" + e.getSha256() + " current=" + now);
        return e;
    }

    public Resource download(Long id, String user) {
        Evidence e = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
        audit.log(user, "DOWNLOAD", id, "File=" + e.getOriginalFileName());
        return new FileSystemResource(dir.resolve(e.getStoredFileName()));
    }

    public Evidence meta(Long id) { return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found")); }

    public void delete(Long id, String user) throws IOException {
        Evidence e = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Evidence not found"));
        Files.deleteIfExists(dir.resolve(e.getStoredFileName()));
        repo.delete(e);
        cache.evict("evidence:" + id);
        invalidateLists(e.getCaseId());
        audit.log(user, "DELETE", id, "File=" + e.getOriginalFileName() + " SHA256=" + e.getSha256());
    }

    private void invalidateLists(Long caseId) {
        cache.evict("case:" + caseId + ":evidence", "evidence:all");
        cache.evictPattern("search:*");
    }

    private static double ms(long t0) { return Math.round((System.nanoTime() - t0) / 10_000.0) / 100.0; }
}
