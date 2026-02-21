package com.demo.knowledgebase.config;

import com.demo.knowledgebase.model.Role;
import com.demo.knowledgebase.model.User;
import com.demo.knowledgebase.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Initializes the database with a default admin user.
 */
@Configuration
public class DataInitializer {

        @Bean
        CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
                return args -> {
                        // Create default admin if not exists
                        if (!userRepository.existsByUsername("admin")) {
                                User admin = new User();
                                admin.setUsername("admin");
                                admin.setEmail("admin@example.com");
                                admin.setPassword(passwordEncoder.encode("admin123"));
                                admin.setRole(Role.ADMIN);
                                admin.setEnabled(true);
                                userRepository.save(admin);
                                System.out.println(
                                                "✓ Default admin user created (username: admin, password: admin123)");
                        }

                        // Create a demo user if not exists
                        if (!userRepository.existsByUsername("user")) {
                                User user = new User();
                                user.setUsername("user");
                                user.setEmail("user@example.com");
                                user.setPassword(passwordEncoder.encode("user123"));
                                user.setRole(Role.USER);
                                user.setEnabled(true);
                                userRepository.save(user);
                                System.out.println("✓ Default user created (username: user, password: user123)");
                        }

                        // Create additional admin 'eyad' if not exists
                        if (!userRepository.existsByUsername("eyad")) {
                                User eyad = new User();
                                eyad.setUsername("eyad");
                                eyad.setEmail("eyad@example.com");
                                eyad.setPassword(passwordEncoder.encode("eyad12"));
                                eyad.setRole(Role.ADMIN);
                                eyad.setEnabled(true);
                                userRepository.save(eyad);
                                System.out.println("✓ Additional admin created (username: eyad, password: eyad12)");
                        }
                };
        }
}
