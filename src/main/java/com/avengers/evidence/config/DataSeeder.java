package com.avengers.evidence.config;

import com.avengers.evidence.model.*;
import com.avengers.evidence.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users; private final CaseRepository cases; private final PasswordEncoder enc;

    public DataSeeder(UserRepository users, CaseRepository cases, PasswordEncoder enc) {
        this.users = users; this.cases = cases; this.enc = enc;
    }

    @Override public void run(String... args) {
        if (users.count() == 0) {
            users.save(new AppUser("admin", enc.encode("admin123"), "System Administrator", Role.ADMIN));
            users.save(new AppUser("investigator", enc.encode("invest123"), "Lead Investigator", Role.INVESTIGATOR));
        }
        if (cases.count() == 0) {
            CaseFile c = new CaseFile();
            c.setCaseNumber("CASE-2026-001");
            c.setTitle("Online Banking Fraud - Dehradun");
            c.setDescription("Phishing campaign targeting local bank customers. CCTV, chat logs and scanned complaints collected.");
            c.setCreatedBy("admin");
            cases.save(c);
        }
    }
}
