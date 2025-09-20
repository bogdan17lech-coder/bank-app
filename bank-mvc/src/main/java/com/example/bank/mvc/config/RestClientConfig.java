package com.example.bank.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    // RestTemplate with optional auth (basic/bearer) + simple request logging
    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${bank.api.auth.type:none}") String type,
            @Value("${bank.api.auth.username:}") String username,
            @Value("${bank.api.auth.password:}") String password,
            @Value("${bank.api.auth.token:}") String token
    ) {
        RestTemplateBuilder b = builder;

        // Add auth if configured
        if ("basic".equalsIgnoreCase(type)) {
            b = b.basicAuthentication(username, password);
        } else if ("bearer".equalsIgnoreCase(type)) {
            final String authValue = "Bearer " + token;
            b = b.additionalInterceptors((req, body, ex) -> {
                req.getHeaders().add(HttpHeaders.AUTHORIZATION, authValue);
                return ex.execute(req, body);
            });
        }

        // Tiny debug logger for outgoing requests (use proper logger in prod)
        b = b.additionalInterceptors((req, body, ex) -> {
            System.out.println("RestTemplate → " + req.getMethod() + " " + req.getURI());
            return ex.execute(req, body);
        });

        return b.build();
    }
}
