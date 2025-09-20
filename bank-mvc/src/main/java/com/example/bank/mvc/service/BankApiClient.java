package com.example.bank.mvc.service;

import com.example.bank.mvc.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Thin REST client for the backend API (used by MVC layer).
 * Keeps payloads minimal and maps responses into MVC DTOs.
 */
@Service
public class BankApiClient {

    @Value("${bank.api.base-url}")
    private String baseUrl;

    @Value("${bank.api.customers-path:/api/customers}")
    private String customersPath;

    private final RestTemplate rest;

    public BankApiClient(RestTemplate rest) { this.rest = rest; }

    // ---------- Customers ----------
    public List<CustomerDto> getCustomers() {
        String url = baseUrl + customersPath;
        ResponseEntity<CustomerDto[]> resp = rest.getForEntity(url, CustomerDto[].class);
        List<CustomerDto> list = Arrays.asList(Objects.requireNonNull(resp.getBody()));
        for (CustomerDto c : list) setFullName(c); // compute full name for views
        return list;
    }

    public CustomerDto getCustomer(Long id) {
        String url = baseUrl + customersPath + "/" + id;
        CustomerDto c = Objects.requireNonNull(rest.getForObject(url, CustomerDto.class));
        setFullName(c);
        return c;
    }

    public CustomerDto createCustomer(CustomerDto form) {
        String url = baseUrl + customersPath;
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", form.getFirstName());
        payload.put("lastName",  form.getLastName());
        payload.put("email",     form.getEmail());
        ResponseEntity<CustomerDto> resp = rest.postForEntity(url, payload, CustomerDto.class);
        CustomerDto body = resp.getBody();
        if (body != null) setFullName(body);
        return body;
    }

    public CustomerDto updateCustomer(Long id, CustomerDto form) {
        String url = baseUrl + customersPath + "/" + id;
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", form.getFirstName());
        payload.put("lastName",  form.getLastName());
        payload.put("email",     form.getEmail());
        rest.put(url, payload);
        return getCustomer(id); // re-fetch to get updated + computed fullName
    }

    public void deleteCustomer(Long id) {
        rest.delete(baseUrl + customersPath + "/" + id);
    }

    // Build "First Last" for MVC (not persisted by API)
    private void setFullName(CustomerDto c) {
        String fn = c.getFirstName() == null ? "" : c.getFirstName();
        String ln = c.getLastName()  == null ? "" : c.getLastName();
        c.setFullName((fn + " " + ln).trim());
    }

    // ---------- Accounts (nested under customer) ----------
    public List<AccountDto> getAccountsByCustomer(Long customerId) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts";
        ResponseEntity<AccountDto[]> resp = rest.getForEntity(url, AccountDto[].class);
        return Arrays.asList(Objects.requireNonNull(resp.getBody()));
    }

    public AccountDto getAccount(Long customerId, Long accountId) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + accountId;
        return Objects.requireNonNull(rest.getForObject(url, AccountDto.class));
    }

    public List<TransactionDto> getAccountTransactions(Long customerId, Long accountId) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + accountId + "/transactions";
        ResponseEntity<TransactionDto[]> resp = rest.getForEntity(url, TransactionDto[].class);
        return Arrays.asList(Objects.requireNonNull(resp.getBody()));
    }

    // Create account with number + currency + initial balance (as required by API)
    public AccountDto createAccount(Long customerId, String number, String currency, BigDecimal balance) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts";
        Map<String, Object> payload = new HashMap<>();
        payload.put("number", number);
        payload.put("currency", currency);
        payload.put("balance", balance); // initial balance sent to REST
        return Objects.requireNonNull(rest.postForObject(url, payload, AccountDto.class));
    }

    public void deleteAccount(Long customerId, Long accountId) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + accountId;
        rest.delete(url);
    }

    public TransactionDto deposit(Long customerId, Long accountId, BigDecimal amount, String description) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + accountId + "/deposit";
        Map<String, Object> payload = Map.of("amount", amount, "description", description);
        return Objects.requireNonNull(rest.postForObject(url, payload, TransactionDto.class));
    }

    public TransactionDto withdraw(Long customerId, Long accountId, BigDecimal amount, String description) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + accountId + "/withdraw";
        Map<String, Object> payload = Map.of("amount", amount, "description", description);
        return Objects.requireNonNull(rest.postForObject(url, payload, TransactionDto.class));
    }

    // Assume REST allows cross-customer transfers by toAccountId
    public TransactionDto transfer(Long customerId, Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {
        String url = baseUrl + customersPath + "/" + customerId + "/accounts/" + fromAccountId + "/transfer";
        Map<String, Object> payload = new HashMap<>();
        payload.put("toAccountId", toAccountId);
        payload.put("amount", amount);
        payload.put("description", description);
        return Objects.requireNonNull(rest.postForObject(url, payload, TransactionDto.class));
    }

    // Public read by account id (if backend exposes /api/accounts/{id})
    public AccountDto getAccountByAnyId(Long accountId) {
        String url = baseUrl + "/api/accounts/" + accountId;
        return rest.getForObject(url, AccountDto.class);
    }
}
