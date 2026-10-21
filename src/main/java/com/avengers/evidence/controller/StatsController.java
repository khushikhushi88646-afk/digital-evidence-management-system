package com.avengers.evidence.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.avengers.evidence.service.CacheService;
import com.avengers.evidence.service.StatsService;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    private final StatsService stats; private final CacheService cache;
    public StatsController(StatsService stats, CacheService cache) { this.stats = stats; this.cache = cache; }

    @GetMapping("/overview") public Map<String, Object> overview() { return stats.overview(); }

    @GetMapping("/benchmark")
    public Map<String, Object> benchmark(@RequestParam Long caseId, @RequestParam(defaultValue = "500") int n) {
        return stats.benchmark(caseId, n);
    }

    @PostMapping("/reset") public Map<String, String> reset() { cache.resetCounters(); return Map.of("status", "counters reset"); }
}
