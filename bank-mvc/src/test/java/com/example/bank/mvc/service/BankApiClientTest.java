package com.example.bank.mvc.service;

import com.example.bank.mvc.dto.AccountDto;
import com.example.bank.mvc.dto.CustomerDto;
import com.example.bank.mvc.dto.TransactionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BankApiClientTest {

    private RestTemplate rest;
    private MockRestServiceServer server;
    private BankApiClient api;

    @BeforeEach
    void setUp() {
        rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        api = new BankApiClient(rest);

        // set props
        ReflectionTestUtils.setField(api, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(api, "customersPath", "/api/customers");
    }

    @Test
    @DisplayName("getCustomers sets fullName from first+last")
    void getCustomers_setsFullName() {
        server.expect(requestTo("http://localhost:8080/api/customers"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"firstName\":\"Jane\",\"lastName\":\"Doe\"}]",
                        MediaType.APPLICATION_JSON));

        List<CustomerDto> list = api.getCustomers();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getFullName()).isEqualTo("Jane Doe");
        server.verify();
    }

    @Test
    @DisplayName("createAccount posts number+currency+balance to correct URL")
    void createAccount_payload() {
        server.expect(requestTo("http://localhost:8080/api/customers/1/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.number").value("PL11"))
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.balance").value(123.45))
                .andRespond(withSuccess("{\"id\":555}", MediaType.APPLICATION_JSON));

        AccountDto dto = api.createAccount(1L, "PL11", "PLN", new BigDecimal("123.45"));

        assertThat(dto).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("transfer posts to /customers/{cid}/accounts/{from}/transfer with payload")
    void transfer_payload() {
        server.expect(requestTo("http://localhost:8080/api/customers/5/accounts/10/transfer"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.toAccountId").value(20))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.description").value("note"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        TransactionDto t = api.transfer(5L, 10L, 20L, new BigDecimal("50.00"), "note");

        assertThat(t).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("getAccountByAnyId hits public /api/accounts/{id}")
    void getAccountByAnyId_url() {
        server.expect(requestTo("http://localhost:8080/api/accounts/99"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":99}", MediaType.APPLICATION_JSON));

        AccountDto dto = api.getAccountByAnyId(99L);

        assertThat(dto).isNotNull();
        server.verify();
    }
}
