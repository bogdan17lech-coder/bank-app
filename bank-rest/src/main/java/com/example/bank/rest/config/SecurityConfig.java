package com.example.bank.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Simple security for REST API (dev).
 * - Public GET for read-only endpoints
 * - Basic auth for write endpoints
 * - Allow H2 console (no auth) and disable frames/CSRF for it
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF is not needed for stateless REST and H2 console
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .headers(h -> h.frameOptions(f -> f.disable())) // H2 uses frames
                .authorizeHttpRequests(auth -> auth
                        // H2 console must be reachable without auth
                        .requestMatchers("/h2-console/**").permitAll()

                        // Public read endpoints
                        .requestMatchers(HttpMethod.GET,
                                "/api/customers/**",
                                "/api/accounts/**",
                                "/api/ping"
                        ).permitAll()

                        // Write endpoints require role API
                        .requestMatchers(HttpMethod.POST,   "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.PUT,    "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("API")

                        // Everything else -> auth
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /** In-memory user for dev/tests (do not use in prod). */
    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("api")
                        .password("{noop}secret")
                        .roles("API")
                        .build()
        );
    }
}
