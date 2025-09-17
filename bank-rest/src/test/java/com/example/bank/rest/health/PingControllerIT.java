package com.example.bank.rest.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PingControllerIT {

    @Autowired
    MockMvc mvc;

    private static String basicAuth() {
        var token = java.util.Base64.getEncoder()
                .encodeToString("api:secret".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    @Test
    void ping_ok() throws Exception {
        mvc.perform(get("/api/ping")
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }



}
