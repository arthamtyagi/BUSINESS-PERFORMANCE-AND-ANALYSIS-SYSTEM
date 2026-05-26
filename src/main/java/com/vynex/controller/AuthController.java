package com.vynex.controller;

import com.vynex.dto.SigninRequest;
import com.vynex.dto.SignupRequest;
import com.vynex.model.User;
import com.vynex.repository.UserRepository;
import com.vynex.session.SessionManager;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final SessionManager sessionManager;

    public AuthController(UserRepository userRepository, SessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    // ── POST /api/auth/signup ──────────────────────────────────────────────────
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already registered"));
        }

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(req.getPassword());
        user.setAccountType(req.getAccountType() != null ? req.getAccountType() : "customer");
        user.setCompanyName(req.getCompanyName());
        user.setBusinessType(req.getBusinessType());
        user.setCompanySize(req.getCompanySize() != null ? req.getCompanySize() : 0);
        user.setIndustry(req.getIndustry());

        try {
            userRepository.save(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already registered"));
        }

        return ResponseEntity.ok(Map.of("message", "Account created successfully"));
    }

    // ── POST /api/auth/signin ──────────────────────────────────────────────────
    @PostMapping("/signin")
    public ResponseEntity<Map<String, String>> signin(@Valid @RequestBody SigninRequest req) {
        Optional<User> userOpt = userRepository.findByEmailAndPassword(
                req.getEmail(), req.getPassword());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid email or password"));
        }

        User user = userOpt.get();

        // Create a session token — stored in ConcurrentHashMap, managed by background thread
        String token = sessionManager.createSession(
                user.getEmail(),
                user.getFullName() != null ? user.getFullName() : "",
                user.getAccountType() != null ? user.getAccountType() : "customer"
        );

        return ResponseEntity.ok(Map.of(
                "message",     "Login successful",
                "name",        user.getFullName() != null ? user.getFullName() : "",
                "accountType", user.getAccountType() != null ? user.getAccountType() : "",
                "token",       token
        ));
    }

    // ── GET /api/auth/check-session ───────────────────────────────────────────
    // Frontend calls this periodically to check if session is still alive
    @GetMapping("/check-session")
    public ResponseEntity<Map<String, String>> checkSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(Map.of("status", "no_session"));
        }

        String token = authHeader.substring(7);

        if (!sessionManager.isValid(token)) {
            return ResponseEntity.status(401)
                    .body(Map.of("status", "expired", "message", "Session expired. Please log in again."));
        }

        SessionManager.SessionData data = sessionManager.getSession(token);
        return ResponseEntity.ok(Map.of(
                "status",      "active",
                "name",        data.name(),
                "accountType", data.accountType()
        ));
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            sessionManager.invalidate(authHeader.substring(7));
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
