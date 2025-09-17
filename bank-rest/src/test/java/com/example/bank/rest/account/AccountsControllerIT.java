package com.example.bank.rest.account;

import com.example.bank.rest.customer.CustomerEntity;
import com.example.bank.rest.customer.CustomerRepository;
import com.example.bank.rest.transaction.TransactionEntity;
import com.example.bank.rest.transaction.TransactionRepository;
import com.example.bank.rest.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full-stack IT для /api/customers/{cid}/accounts/* и /api/accounts/{id}. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AccountsControllerIT {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accountRepo;
    @Autowired TransactionRepository trxRepo;
    @Autowired CustomerRepository customerRepo;

    private static String basicAuth() {
        String token = Base64.getEncoder()
                .encodeToString("api:secret".getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private Long makeCustomer(String email) {
        var c = new CustomerEntity();
        c.setFirstName("T");
        c.setLastName("User");
        c.setEmail(email);
        return customerRepo.save(c).getId();
    }

    private AccountEntity makeAccount(long customerId, String number, String currency, String balance) {
        var a = new AccountEntity();
        a.setCustomerId(customerId);
        a.setNumber(number);
        a.setCurrency(currency);
        a.setBalance(new BigDecimal(balance));
        return accountRepo.save(a);
    }

    @Test
    void create_list_get_public_get_delete_ok() throws Exception {
        long cid = makeCustomer("acc1+" + UUID.randomUUID() + "@x");

        String number = "ACC-" + UUID.randomUUID();
        // create с нулевым балансом, чтобы можно было удалить без FK-проблем с транзакциями
        mvc.perform(post("/api/customers/{cid}/accounts", cid)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                        {"number":"%s","currency":"PLN","balance":0.00}
                        """).formatted(number)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.currency").value("PLN"))
                .andExpect(jsonPath("$.balance").value(0.00));

        Long accId = accountRepo.findByNumber(number).orElseThrow().getId();

        // list
        mvc.perform(get("/api/customers/{cid}/accounts", cid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // get (by owner)
        mvc.perform(get("/api/customers/{cid}/accounts/{aid}", cid, accId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accId.intValue()))
                .andExpect(jsonPath("$.currency").value("PLN"));

        // public GET
        mvc.perform(get("/api/accounts/{id}", accId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accId.intValue()));

        // delete
        mvc.perform(delete("/api/customers/{cid}/accounts/{aid}", cid, accId)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_negative_balance_400() throws Exception {
        long cid = makeCustomer("neg+" + UUID.randomUUID() + "@x");

        mvc.perform(post("/api/customers/{cid}/accounts", cid)
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"number":"NEG-1","currency":"PLN","balance":-1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("balance cannot be negative"));
    }

    @Test
    void deposit_and_withdraw_and_transactions() throws Exception {
        long cid = makeCustomer("flow+" + UUID.randomUUID() + "@x");
        var a = makeAccount(cid, "A-" + UUID.randomUUID(), "PLN", "10.00");

        // deposit +15
        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/deposit", cid, a.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"amount":15.00,"description":"topup"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(15.00));

        // withdraw 5
        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/withdraw", cid, a.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"amount":5.00,"description":"atm"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAW"))
                .andExpect(jsonPath("$.amount").value(5.00));

        // balance = 20.00
        var reloaded = accountRepo.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("20.00");

        // history (GET)
        mvc.perform(get("/api/customers/{cid}/accounts/{aid}/transactions", cid, a.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void withdraw_insufficient_funds_400() throws Exception {
        long cid = makeCustomer("no$+" + UUID.randomUUID() + "@x");
        var a = makeAccount(cid, "A-" + UUID.randomUUID(), "PLN", "3.00");

        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/withdraw", cid, a.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"amount":5.00}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("insufficient funds"));
    }

    @Test
    void transfer_currency_mismatch_and_success() throws Exception {
        long fromCid = makeCustomer("from+" + UUID.randomUUID() + "@x");
        long toCid1  = makeCustomer("usd+" + UUID.randomUUID() + "@x");
        long toCid2  = makeCustomer("pln+" + UUID.randomUUID() + "@x");

        // from: PLN 50
        var from = makeAccount(fromCid, "FROM-" + UUID.randomUUID(), "PLN", "50.00");
        // to: USD 0
        var toUsd = makeAccount(toCid1, "USD-" + UUID.randomUUID(), "USD", "0.00");

        // mismatch -> 400
        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/transfer", fromCid, from.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                        {"toAccountId":%d,"amount":10.00,"description":"x"}
                        """).formatted(toUsd.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("currencies must match"));

        // same currency target
        var toPln = makeAccount(toCid2, "PLN-" + UUID.randomUUID(), "PLN", "0.00");

        // success
        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/transfer", fromCid, from.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("""
                        {"toAccountId":%d,"amount":15.00,"description":"rent"}
                        """).formatted(toPln.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$.amount").value(15.00));

        // balances updated
        var reFrom = accountRepo.findById(from.getId()).orElseThrow();
        var reTo   = accountRepo.findById(toPln.getId()).orElseThrow();
        assertThat(reFrom.getBalance()).isEqualByComparingTo("35.00");
        assertThat(reTo.getBalance()).isEqualByComparingTo("15.00");

        // two transaction rows exist (out & in)
        var outTypes = trxRepo.findByAccount_IdOrderByCreatedAtDesc(from.getId())
                .stream().map(TransactionEntity::getType).toList();
        var inTypes  = trxRepo.findByAccount_IdOrderByCreatedAtDesc(toPln.getId())
                .stream().map(TransactionEntity::getType).toList();

        assertThat(outTypes).contains(TransactionType.TRANSFER_OUT);
        assertThat(inTypes).contains(TransactionType.TRANSFER_IN);
    }

    @Test
    void delete_non_zero_balance_409() throws Exception {
        long cid = makeCustomer("del409+" + UUID.randomUUID() + "@x");
        var acc = makeAccount(cid, "D-" + UUID.randomUUID(), "PLN", "5.00"); // баланс не 0

        mvc.perform(delete("/api/customers/{cid}/accounts/{aid}", cid, acc.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("balance must be 0 to delete"));
    }

    @Test
    void create_requires_auth_401() throws Exception {
        long cid = 777_000 + System.nanoTime(); // любой id
        mvc.perform(post("/api/customers/{cid}/accounts", cid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"number":"NOAUTH-%s","currency":"PLN","balance":0}
                    """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deposit_amount_not_positive_400() throws Exception {
        long cid = makeCustomer("dep0+" + java.util.UUID.randomUUID() + "@x");
        var a = makeAccount(cid, "D0-" + java.util.UUID.randomUUID(), "PLN", "10.00");

        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/deposit", cid, a.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))                  // <-- обычная строка
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount must be positive"));
    }

    @Test
    void withdraw_amount_not_positive_400() throws Exception {
        long cid = makeCustomer("wd0+" + java.util.UUID.randomUUID() + "@x");
        var a = makeAccount(cid, "W0-" + java.util.UUID.randomUUID(), "PLN", "10.00");

        mvc.perform(post("/api/customers/{cid}/accounts/{aid}/withdraw", cid, a.getId())
                        .header(HttpHeaders.AUTHORIZATION, basicAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-1}"))                 // <-- обычная строка
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount must be positive"));
    }


    @Test
    void owner_mismatch_get_and_transactions_404() throws Exception {
        long ownerCid = makeCustomer("own+" + java.util.UUID.randomUUID() + "@x");
        long otherCid = makeCustomer("oth+" + java.util.UUID.randomUUID() + "@x");
        var a = makeAccount(ownerCid, "OWN-" + java.util.UUID.randomUUID(), "PLN", "5.00");

        // чужой владелец запрашивает сам счёт
        mvc.perform(get("/api/customers/{cid}/accounts/{aid}", otherCid, a.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("account not found"));

        // и историю транзакций
        mvc.perform(get("/api/customers/{cid}/accounts/{aid}/transactions", otherCid, a.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("account not found"));
    }

    @Test
    void public_account_not_found_404() throws Exception {
        mvc.perform(get("/api/accounts/{id}", 9_999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("account not found"));
    }


}
