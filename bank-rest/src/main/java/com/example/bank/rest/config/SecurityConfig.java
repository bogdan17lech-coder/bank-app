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

/** Basic security for REST API: public GET, authenticated writes */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // For REST API we disable CSRF on /api/**
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // Public read endpoints
                        .requestMatchers(HttpMethod.GET, "/api/customers/**", "/api/accounts/**").permitAll()
                        // Write endpoints require role API
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.PUT,  "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.DELETE,"/api/**").hasRole("API")
                        // Everything else must be authenticated
                        .anyRequest().authenticated()
                )
                // Simple HTTP Basic for demo/dev
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    // In-memory user for local/dev use only
    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("api")
                        .password("{noop}secret") // DO NOT use in production
                        .roles("API")
                        .build()
        );
    }
}
