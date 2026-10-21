package com.avengers.evidence.service;

import com.avengers.evidence.model.Evidence;
import com.avengers.evidence.repo.CaseRepository;
import com.avengers.evidence.repo.EvidenceRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatsService {
    private final EvidenceRepository evidence;
    private final CaseRepository cases;
    private final CacheService cache;

    public StatsService(EvidenceRepository evidence, CaseRepository cases, CacheService cache) {
        this.evidence = evidence; this.cases = cases; this.cache = cache;
    }

    public Map<String, Object> overview() {
        long hits = cache.counter("stats:hits"), misses = cache.counter("stats:misses");
        List<Evidence> all = evidence.findAll();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cases", cases.count());
        m.put("evidence", all.size());
        m.put("verified", evidence.countByIntegrityStatus("VERIFIED"));
        m.put("tampered", evidence.countByIntegrityStatus("TAMPERED"));
        m.put("unverified", evidence.countByIntegrityStatus("UNVERIFIED"));
        m.put("cacheHits", hits);
        m.put("cacheMisses", misses);
        m.put("hitRatio", hits + misses == 0 ? 0 : Math.round(hits * 1000.0 / (hits + misses)) / 10.0);
        m.put("redisKeys", cache.dbSize());
        m.put("redisMemory", cache.info("memory", "used_memory_human"));
        m.put("redisVersion", cache.info("server", "redis_version"));
        m.put("activeSessions", cache.activeSessions());
        m.put("byType", all.stream().collect(Collectors.groupingBy(
                e -> e.getEvidenceType() == null ? "OTHER" : e.getEvidenceType(), LinkedHashMap::new, Collectors.counting())));
        return m;
    }

    /** Proposal deliverable #5: response time WITH vs WITHOUT Redis. */
    public Map<String, Object> benchmark(Long caseId, int n) {
        n = Math.max(10, Math.min(n, 5000));
        // warm-up (JIT + connection pools) so the comparison is fair
        for (int i = 0; i < 20; i++) { evidence.findByCaseIdOrderByUploadedAtDesc(caseId); }
        String key = "bench:case:" + caseId;
        cache.put(key, evidence.findByCaseIdOrderByUploadedAtDesc(caseId));
        for (int i = 0; i < 20; i++) cache.getQuiet(key, cache.listOf(Evidence.class));

        long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) evidence.findByCaseIdOrderByUploadedAtDesc(caseId);
        double dbMs = (System.nanoTime() - t0) / 1e6;

        t0 = System.nanoTime();
        for (int i = 0; i < n; i++) cache.getQuiet(key, cache.listOf(Evidence.class));
        double redisMs = (System.nanoTime() - t0) / 1e6;
        cache.evict(key);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("requests", n);
        m.put("dbTotalMs", Math.round(dbMs * 100) / 100.0);
        m.put("redisTotalMs", Math.round(redisMs * 100) / 100.0);
        m.put("dbAvgMs", Math.round(dbMs / n * 1000) / 1000.0);
        m.put("redisAvgMs", Math.round(redisMs / n * 1000) / 1000.0);
        m.put("speedup", redisMs == 0 ? 0 : Math.round(dbMs / redisMs * 100) / 100.0);
        return m;
    }
}
