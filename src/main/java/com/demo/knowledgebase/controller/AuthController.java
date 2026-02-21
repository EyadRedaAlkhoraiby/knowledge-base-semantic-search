package com.demo.knowledgebase.controller;

import com.demo.knowledgebase.model.Role;
import com.demo.knowledgebase.model.User;
import com.demo.knowledgebase.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication controller for registration and user info.
 * Login/logout are handled by Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Check if username exists
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already exists"));
        }

        // Create new user
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER); // Default role
        user.setEnabled(true);

        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "User registered successfully",
                        "username", user.getUsername()));
    }

    /**
     * Get current logged-in user info.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "isAdmin", user.getRole() == Role.ADMIN));
    }

    /**
     * Check if user is authenticated.
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean authenticated = auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal());

        return ResponseEntity.ok(Map.of("authenticated", authenticated));
    }

    // Request DTOs
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6) String password) {
    }
}
