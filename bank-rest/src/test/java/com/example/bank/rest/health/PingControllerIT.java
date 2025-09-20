package com.example.bank.rest.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack IT for /api/ping (health endpoint).
 * Uses TEST profile (in-memory H2). DB is not used here, but profile is consistent.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class PingControllerIT {

    @Autowired MockMvc mvc;

    @Test
    void ping_returns_ok_json() throws Exception {
        // Expect 200 + JSON body {"status":"ok"}
        mvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
