package com.demo.knowledgebase.config;

import com.demo.knowledgebase.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration.
 * 
 * Updated for Angular SPA frontend:
 * - CORS enabled for Angular dev server (localhost:4200)
 * - Login success/failure return JSON responses instead of HTML redirects
 * - Angular static assets (.js, .css, etc.) are publicly accessible
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // Disable for API access
                .authorizeHttpRequests(auth -> auth
                        // Public resources - Angular & legacy
                        .requestMatchers("/", "/index.html", "/login", "/login.html",
                                "/register", "/register.html", "/dashboard")
                        .permitAll()
                        .requestMatchers("/css/**", "/js/**", "/styles.css", "/app.js", "/auth.js").permitAll()
                        .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.woff2",
                                "/*.woff", "/*.ttf", "/assets/**", "/media/**")
                        .permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/check").permitAll()
                        .requestMatchers("/h2-console/**").permitAll() // H2 console for dev

                        // Read operations - authenticated users
                        .requestMatchers(HttpMethod.GET, "/api/documents", "/api/documents/**").authenticated()
                        .requestMatchers("/api/search", "/api/stats", "/api/categories").authenticated()

                        // Write operations - ADMIN only
                        .requestMatchers(HttpMethod.POST, "/api/documents").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/documents/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**").hasRole("ADMIN")
                        .requestMatchers("/api/import").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":\"success\"}");
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":\"error\",\"message\":\"Invalid credentials\"}");
                        })
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":\"logged_out\"}");
                        })
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretKey12345")
                        .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 days
                        .userDetailsService(userDetailsService))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin()) // For H2 console
                );

        return http.build();
    }
}
