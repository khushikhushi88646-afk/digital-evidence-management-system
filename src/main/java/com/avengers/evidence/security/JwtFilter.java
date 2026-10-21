package com.avengers.evidence.security;

import com.avengers.evidence.service.CacheService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Validates the JWT AND checks that its session still exists in Redis (so logout really logs out). */
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final CacheService cache;

    public JwtFilter(JwtService jwt, CacheService cache) { this.jwt = jwt; this.cache = cache; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            try {
                Claims c = jwt.parse(h.substring(7));
                if (cache.hasSession(c.getId())) {
                    var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), c.getId(),
                            List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class))));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) { /* invalid/expired -> stays unauthenticated -> 401 */ }
        }
        chain.doFilter(req, res);
    }
}
