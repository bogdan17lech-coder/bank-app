package com.example.bank.rest.customer;

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

import static org.hamcrest.Matchers.isEmptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CustomersControllerIT {

    @Autowired MockMvc mvc;
    @Autowired CustomerRepository repo;

    private static String basicAuth() {
        String token = Base64.getEncoder()
                .encodeToString("api:secret".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    @Test
    void create_get_delete_ok() throws Exception {
        // уникальный email, чтобы не зависеть от состояния БД
        String email = "jane+" + UUID.randomUUID() + "@example.com";

        // CREATE (POST /api/customers)
        mvc.perform(post("/api/customers")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                    {"firstName":"Jane","lastName":"Doe","email":"%s"}
                """).formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value(email));

        // берем id из репозитория
        Long id = repo.findByEmail(email).orElseThrow().getId();

        // GET (публичный)
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.email").value(email));

        // DELETE (нужен basic-auth)
        mvc.perform(delete("/api/customers/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isNoContent());

        // после удаления — 404
        mvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("customer not found"));
    }

    @Test
    void create_requires_auth_401() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"firstName":"No","lastName":"Auth","email":"noauth+%s@x"}
                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

}
