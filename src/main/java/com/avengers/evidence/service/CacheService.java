package com.avengers.evidence.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Thin Redis wrapper: JSON get/put with TTL, pattern-invalidation and hit/miss counters. */
@Service
public class CacheService {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final long ttl;

    public CacheService(StringRedisTemplate redis, ObjectMapper mapper, @Value("${app.cache.ttl-seconds}") long ttl) {
        this.redis = redis; this.mapper = mapper; this.ttl = ttl;
    }

    /** Same as get() but does not touch hit/miss counters (used by the benchmark). */
    public <T> Optional<T> getQuiet(String key, JavaType type) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? Optional.empty() : Optional.of(mapper.readValue(json, type));
        } catch (Exception e) { return Optional.empty(); }
    }

    public <T> Optional<T> get(String key, JavaType type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) { redis.opsForValue().increment("stats:misses"); return Optional.empty(); }
            redis.opsForValue().increment("stats:hits");
            return Optional.of(mapper.readValue(json, type));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void put(String key, Object value) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(value), Duration.ofSeconds(ttl));
        } catch (Exception ignored) { }
    }

    public void evict(String... keys) { redis.delete(List.of(keys)); }

    public void evictPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        try (Cursor<String> c = redis.scan(ScanOptions.scanOptions().match(pattern).count(200).build())) {
            c.forEachRemaining(keys::add);
        }
        if (!keys.isEmpty()) redis.delete(keys);
    }

    public JavaType type(Class<?> c) { return mapper.getTypeFactory().constructType(c); }
    public JavaType listOf(Class<?> c) { return mapper.getTypeFactory().constructCollectionType(List.class, c); }

    public long counter(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? 0 : Long.parseLong(v);
    }
    public void resetCounters() { redis.delete(List.of("stats:hits", "stats:misses")); }

    public long dbSize() {
        Long n = redis.execute((RedisCallback<Long>) c -> c.serverCommands().dbSize());
        return n == null ? 0 : n;
    }
    public String info(String section, String field) {
        try {
            java.util.Properties p = redis.execute((RedisCallback<java.util.Properties>) c -> c.serverCommands().info(section));
            return p == null ? "" : p.getProperty(field, "");
        } catch (Exception e) { return ""; }
    }

    // ---- session store (JWT sessions live in Redis)
    public void putSession(String jti, String username, long minutes) {
        redis.opsForValue().set("session:" + jti, username, Duration.ofMinutes(minutes));
    }
    public boolean hasSession(String jti) { return Boolean.TRUE.equals(redis.hasKey("session:" + jti)); }
    public void dropSession(String jti) { redis.delete("session:" + jti); }
    public long activeSessions() {
        long n = 0;
        try (Cursor<String> c = redis.scan(ScanOptions.scanOptions().match("session:*").count(200).build())) {
            while (c.hasNext()) { c.next(); n++; }
        }
        return n;
    }
}
