package com.avengers.evidence.controller;

import com.avengers.evidence.model.AppUser;
import com.avengers.evidence.model.Role;
import com.avengers.evidence.repo.UserRepository;
import com.avengers.evidence.security.JwtService;
import com.avengers.evidence.service.AuditService;
import com.avengers.evidence.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users; private final PasswordEncoder enc; private final JwtService jwt;
    private final CacheService cache; private final AuditService audit;

    public AuthController(UserRepository users, PasswordEncoder enc, JwtService jwt, CacheService cache, AuditService audit) {
        this.users = users; this.enc = enc; this.jwt = jwt; this.cache = cache; this.audit = audit;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String u = body.getOrDefault("username", "").trim(), p = body.getOrDefault("password", "");
        var user = users.findByUsername(u).filter(x -> enc.matches(p, x.getPasswordHash()));
        if (user.isEmpty()) {
            audit.log(u.isEmpty() ? "unknown" : u, "LOGIN_FAILED", null, "Bad credentials");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
        AppUser a = user.get();
        String jti = jwt.newJti();
        cache.putSession(jti, a.getUsername(), jwt.expiryMinutes());
        audit.log(a.getUsername(), "LOGIN", null, "Session " + jti.substring(0, 8));
        return ResponseEntity.ok(Map.of("token", jwt.create(a.getUsername(), a.getRole().name(), jti),
                "username", a.getUsername(), "fullName", a.getFullName(), "role", a.getRole().name()));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(Authentication auth) {
        cache.dropSession((String) auth.getCredentials());
        audit.log(auth.getName(), "LOGOUT", null, "Session removed from Redis");
        return Map.of("status", "logged out");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> b, Authentication auth) {
        String u = b.getOrDefault("username", "").trim();
        if (u.length() < 3 || b.getOrDefault("password", "").length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Username >= 3 and password >= 6 chars required"));
        if (users.findByUsername(u).isPresent()) return ResponseEntity.badRequest().body(Map.of("error", "User exists"));
        Role r = "ADMIN".equals(b.get("role")) ? Role.ADMIN : Role.INVESTIGATOR;
        users.save(new AppUser(u, enc.encode(b.get("password")), b.getOrDefault("fullName", u), r));
        audit.log(auth.getName(), "USER_CREATED", null, "User=" + u + " role=" + r);
        return ResponseEntity.ok(Map.of("status", "created"));
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("username", auth.getName(), "role", auth.getAuthorities().iterator().next().getAuthority().substring(5));
    }
}
