package com.example.bank.mvc.web;

import com.example.bank.mvc.dto.AccountDto;
import com.example.bank.mvc.dto.CustomerDto;
import com.example.bank.mvc.service.BankApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;          // <-- get/post и пр.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;     // <-- status/view/model/flash

/**
 * Standalone MVC tests for CustomersController без Mockito/ByteBuddy.
 */
class CustomersControllerTest {

    private MockMvc mvc;
    private FakeApi api;

    // ---------- helpers ----------
    private static CustomerDto customer(long id, String email, String name) {
        var c = new CustomerDto();
        c.setId(id);
        c.setEmail(email);
        c.setFullName(name);
        return c;
    }

    private static AccountDto account(long id) {
        var a = new AccountDto();
        a.setId(id);
        return a;
    }

    @BeforeEach
    void setUp() {
        api = new FakeApi();
        mvc = MockMvcBuilders.standaloneSetup(new CustomersController(api)).build();
    }

    // ---------- list & view ----------

    @Test
    @DisplayName("GET /customers: ok и ошибка")
    void list_ok_and_error() throws Exception {
        api.customers = new ArrayList<>(List.of(customer(1L, "a@b.com", "John")));

        mvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attribute("customers", org.hamcrest.Matchers.hasSize(1)));

        api.getCustomersThrows = new RuntimeException("down");

        mvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attribute("customers", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(model().attribute("error",
                        org.hamcrest.Matchers.startsWith("Failed to load customers:")));
    }

    @Test
    @DisplayName("GET /customers/{id}: ok и ошибка")
    void view_ok_and_error() throws Exception {
        long id = 1L;
        api.customerById.put(id, customer(id, "x@y", "John"));
        api.accountsByCustomer.put(id, List.of(account(11L), account(12L)));

        mvc.perform(get("/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/view"))
                .andExpect(model().attributeExists("customer", "accounts"));

        api.getCustomerThrows = new RuntimeException("boom");

        mvc.perform(get("/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/view"))
                .andExpect(model().attributeExists("error"));
    }

    // ---------- create ----------

    @Test
    @DisplayName("GET /customers/new: пустая форма")
    void newForm_ok() throws Exception {
        mvc.perform(get("/customers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/new"))
                .andExpect(model().attributeExists("customer"));
    }

    @Test
    @DisplayName("POST create: success → redirect")
    void create_ok() throws Exception {
        api.createResult = customer(77L, "ok@x", "Ok");

        mvc.perform(post("/customers")
                        .param("firstName", "A")
                        .param("lastName", "B")
                        .param("email", "ok@x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/77"));
    }

    @Test
    @DisplayName("POST create: 409 с message → emailError")
    void create_conflict_withMessage() throws Exception {
        String json = "{\"message\":\"Email already exists (from API)\"}";
        api.createThrows = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers").param("email", "dup@x"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/new"))
                .andExpect(model().attribute("emailError", "Email already exists (from API)"));
    }

    @Test
    @DisplayName("POST create: 400 → model error")
    void create_otherHttpError() throws Exception {
        api.createThrows = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad", HttpHeaders.EMPTY,
                "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers").param("email", "bad@x"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/new"))
                .andExpect(model().attribute("error", containsString("Failed to create: 400")));
    }

    @Test
    @DisplayName("POST create: generic → model error")
    void create_genericError() throws Exception {
        api.createThrows = new RuntimeException("down");

        mvc.perform(post("/customers").param("email", "e@x"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/new"))
                .andExpect(model().attribute("error",
                        org.hamcrest.Matchers.startsWith("Failed to create: ")));
    }

    // ---------- edit ----------

    @Test
    @DisplayName("GET edit: ok; при ошибке → назад к list")
    void editForm_ok_and_error() throws Exception {
        long id = 5L;
        api.customerById.put(id, customer(id, "e@x", "John"));

        mvc.perform(get("/customers/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/edit"))
                .andExpect(model().attributeExists("customer"));

        api.getCustomerThrows = new RuntimeException("no");

        mvc.perform(get("/customers/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/list"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeExists("customers"));
    }

    @Test
    @DisplayName("POST edit: ok / 409 / 400 / generic")
    void edit_post() throws Exception {
        long id = 9L;
        api.updateResult = customer(id, "u@x", "U");

        mvc.perform(post("/customers/{id}/edit", id)
                        .param("firstName", "F")
                        .param("lastName", "L")
                        .param("email", "u@x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + id));

        // 409
        api.updateThrows = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers/{id}/edit", 1L))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/edit"))
                .andExpect(model().attribute("emailError", "Email already exists."));

        // 400
        api.updateThrows = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad", HttpHeaders.EMPTY,
                "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers/{id}/edit", 2L))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/edit"))
                .andExpect(model().attribute("error", containsString("Failed to update: 400")));

        // generic
        api.updateThrows = new RuntimeException("down");

        mvc.perform(post("/customers/{id}/edit", 3L))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/edit"))
                .andExpect(model().attribute("error",
                        org.hamcrest.Matchers.startsWith("Failed to update: ")));
    }

    // ---------- delete ----------

    @Test
    @DisplayName("GET delete confirm: ok и ошибка")
    void confirmDelete_ok_and_error() throws Exception {
        long id = 3L;
        api.customerById.put(id, customer(id, "z@x", "Z"));

        mvc.perform(get("/customers/{id}/delete", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/delete"))
                .andExpect(model().attributeExists("customer"));

        api.getCustomerThrows = new RuntimeException("nope");

        mvc.perform(get("/customers/{id}/delete", id))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/view"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("POST delete: ok")
    void doDelete_ok() throws Exception {
        mvc.perform(post("/customers/{id}/delete", 4L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"))
                .andExpect(flash().attribute("message", "Customer deleted."));
    }

    @Test
    @DisplayName("POST delete: 409 с body.message → используем её")
    void doDelete_httpError_withMessage() throws Exception {
        String body = "{\"message\":\"Cannot delete: has linked accounts\"}";
        api.deleteThrows = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers/{id}/delete", 5L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"))
                .andExpect(flash().attribute("error", "Cannot delete: has linked accounts"));
    }

    @Test
    @DisplayName("POST delete: 409 без message → дефолтный хинт")
    void doDelete_httpError_withoutMessage() throws Exception {
        api.deleteThrows = HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY,
                "{}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        mvc.perform(post("/customers/{id}/delete", 6L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"))
                .andExpect(flash().attribute("error",
                        containsString("Likely the customer has linked accounts")));
    }

    @Test
    @DisplayName("POST delete: generic → flash error")
    void doDelete_genericError() throws Exception {
        api.deleteThrows = new RuntimeException("down");

        mvc.perform(post("/customers/{id}/delete", 7L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.startsWith("Failed to delete: ")));
    }

    // ======================================================================
    //                              Fake API
    // ======================================================================
    static class FakeApi extends BankApiClient {
        FakeApi() { super(new RestTemplate()); }

        // data
        List<CustomerDto> customers = new ArrayList<>();
        Map<Long, CustomerDto> customerById = new HashMap<>();
        Map<Long, List<AccountDto>> accountsByCustomer = new HashMap<>();

        // failures
        RuntimeException getCustomersThrows;
        RuntimeException getCustomerThrows;
        RuntimeException createThrows;
        RuntimeException updateThrows;
        RuntimeException deleteThrows;

        // results
        CustomerDto createResult;
        CustomerDto updateResult;

        @Override public List<CustomerDto> getCustomers() {
            if (getCustomersThrows != null) throw getCustomersThrows;
            return customers;
        }

        @Override public CustomerDto getCustomer(Long id) {
            if (getCustomerThrows != null) throw getCustomerThrows;
            return customerById.get(id);
        }

        @Override public CustomerDto createCustomer(CustomerDto form) {
            if (createThrows != null) throw createThrows;
            return createResult;
        }

        @Override public CustomerDto updateCustomer(Long id, CustomerDto form) {
            if (updateThrows != null) throw updateThrows;
            return updateResult;
        }

        @Override public void deleteCustomer(Long id) {
            if (deleteThrows instanceof HttpClientErrorException) throw (HttpClientErrorException) deleteThrows;
            if (deleteThrows instanceof HttpServerErrorException) throw (HttpServerErrorException) deleteThrows;
            if (deleteThrows != null) throw deleteThrows;
        }

        @Override public List<AccountDto> getAccountsByCustomer(Long customerId) {
            return accountsByCustomer.getOrDefault(customerId, List.of());
        }
    }
}
