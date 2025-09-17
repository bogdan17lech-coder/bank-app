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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // для REST: CSRF не нужен
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        // читать можно всем
                        .requestMatchers(HttpMethod.GET, "/api/customers/**", "/api/accounts/**").permitAll()
                        // писать — только авторизованным (роль API)
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.PUT,  "/api/**").hasRole("API")
                        .requestMatchers(HttpMethod.DELETE,"/api/**").hasRole("API")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    public UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("api")
                        .password("{noop}secret") // только для разработки
                        .roles("API")
                        .build()
        );
    }
}
