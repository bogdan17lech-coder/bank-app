package com.example.bank.rest.customer;

import org.junit.jupiter.api.BeforeEach; // JUnit 5
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack IT for /api/customers endpoints.
 * Uses TEST profile (in-memory H2). Each test starts from a clean DB.
 */
@ActiveProfiles("test")   // use application-test.yml with in-memory H2
@SpringBootTest
@AutoConfigureMockMvc
class CustomersControllerIT {

    @Autowired MockMvc mvc;
    @Autowired CustomerRepository repo;

    // Basic auth header for write ops (POST/PUT/DELETE require role API)
    private static String basicAuth() {
        String token = Base64.getEncoder()
                .encodeToString("api:secret".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    // Clean DB before each test (no data leaks between tests)
    @BeforeEach
    void cleanDb() {
        repo.deleteAll();
    }

    @Test
    void create_and_get_ok() throws Exception {
        String email = "c+" + UUID.randomUUID() + "@example.com";

        // Create
        mvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((""" 
                            {"firstName":"John","lastName":"Doe","email":"%s"}
                        """).formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(email));

        Long id = repo.findByEmail(email).orElseThrow().getId();

        // Get by id
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void create_conflict_on_duplicate_email() throws Exception {
        String email = "dup+" + UUID.randomUUID() + "@example.com";

        // First create
        mvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((""" 
                            {"firstName":"A","lastName":"B","email":"%s"}
                        """).formatted(email)))
                .andExpect(status().isCreated());

        // Second create with same email -> 409
        mvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((""" 
                            {"firstName":"C","lastName":"D","email":"%s"}
                        """).formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("email already exists"));
    }

    @Test
    void update_and_delete_flow() throws Exception {
        String email = "u+" + UUID.randomUUID() + "@example.com";

        // Create
        mvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((""" 
                            {"firstName":"Ann","lastName":"Old","email":"%s"}
                        """).formatted(email)))
                .andExpect(status().isCreated());

        Long id = repo.findByEmail(email).orElseThrow().getId();

        // Full update (PUT)
        mvc.perform(put("/api/customers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content((""" 
                            {"firstName":"Ann","lastName":"New","email":"%s"}
                        """).formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("New"));

        // Delete
        mvc.perform(delete("/api/customers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isNoContent());

        // After delete -> 404
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("customer not found"));
    }
}
