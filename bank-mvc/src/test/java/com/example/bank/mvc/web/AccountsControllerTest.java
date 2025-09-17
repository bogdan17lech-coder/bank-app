package com.example.bank.mvc.web;

import com.example.bank.mvc.dto.AccountDto;
import com.example.bank.mvc.dto.CustomerDto;
import com.example.bank.mvc.dto.TransactionDto;
import com.example.bank.mvc.service.BankApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MVC tests with a hand-written FakeApi (no Mockito / no ByteBuddy).
 */
class AccountsControllerTest {

    private MockMvc mvc;
    private FakeApi api;

    // ---------- tiny helpers ----------
    private static CustomerDto customer(long id, String name) {
        var c = new CustomerDto();
        c.setId(id);
        c.setFullName(name);
        return c;
    }

    private static AccountDto account(long id, String currency, String balance) {
        var a = new AccountDto();
        a.setId(id);
        a.setCurrency(currency);
        a.setBalance(balance == null ? null : new BigDecimal(balance));
        return a;
    }

    // ---------- setup ----------
    @BeforeEach
    void setUp() {
        api = new FakeApi();
        mvc = MockMvcBuilders.standaloneSetup(new AccountsController(api)).build();
    }

    // ---------- tests ----------

    @Test @DisplayName("GET /accounts -> redirect:/customers")
    void redirectAccounts() throws Exception {
        mvc.perform(get("/accounts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"));
    }

    @Test @DisplayName("GET list: returns view and model")
    void list_ok() throws Exception {
        long cid = 10L;
        api.customers.put(cid, customer(cid, "John"));
        api.accountsByCustomer.put(cid, List.of(
                account(1L, "PLN", "100.00"),
                account(2L, "PLN", "50.00")
        ));

        mvc.perform(get("/customers/{cid}/accounts", cid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/list"))
                .andExpect(model().attributeExists("customer", "accounts"))
                .andExpect(model().attribute("accounts", hasSize(2)));
    }

    @Test @DisplayName("GET view: loads transactions; on error -> empty list")
    void view_okAndError() throws Exception {
        long cid = 1L, aid = 2L;
        api.customers.put(cid, customer(cid, "A"));
        api.putAccount(cid, account(aid, "PLN", "77.00"));

        // ok branch
        api.transactions.put(FakeApi.key(cid, aid), List.of(new TransactionDto()));
        mvc.perform(get("/customers/{cid}/accounts/{aid}", cid, aid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/view"))
                .andExpect(model().attribute("transactions", hasSize(1)));

        // error branch
        api.transactionsThrow = new RuntimeException("boom");
        mvc.perform(get("/customers/{cid}/accounts/{aid}", cid, aid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/view"))
                .andExpect(model().attribute("transactions", hasSize(0)));
    }

    @Test @DisplayName("GET new account form shows customer")
    void newForm_ok() throws Exception {
        long cid = 5L;
        api.customers.put(cid, customer(cid, "B"));

        mvc.perform(get("/customers/{cid}/accounts/new", cid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/new"))
                .andExpect(model().attributeExists("customer"));
    }

    @Test @DisplayName("POST create: rejects negative initial balance")
    void create_negativeBalance() throws Exception {
        long cid = 5L;

        mvc.perform(post("/customers/{cid}/accounts", cid)
                        .param("number", "X")
                        .param("currency", "PLN")
                        .param("balance", "-1.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/new"))
                .andExpect(flash().attribute("error", "Initial balance cannot be negative."));

        // ensure fake didn't receive create call
        assert api.createCalled == false;
    }

    @Test @DisplayName("POST create: success -> redirect to account view")
    void create_ok() throws Exception {
        long cid = 7L;
        api.createResult = account(99L, "PLN", "0");

        mvc.perform(post("/customers/{cid}/accounts", cid)
                        .param("number", "N")
                        .param("currency", "PLN")
                        .param("balance", "0.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/99"))
                .andExpect(flash().attribute("message", "Account created."));

        assert api.createCalled;
    }

    @Test @DisplayName("POST create: exception -> redirect to /customers/{id}")
    void create_exception() throws Exception {
        long cid = 8L;
        api.createThrows = new RuntimeException("err");

        mvc.perform(post("/customers/{cid}/accounts", cid)
                        .param("number", "N")
                        .param("currency", "PLN")
                        .param("balance", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.startsWith("Failed to create account:")));
    }

    @Test @DisplayName("GET confirm delete: canDelete only for zero/null")
    void confirmDelete() throws Exception {
        long cid = 1L, aid = 2L;
        api.customers.put(cid, customer(cid, "X"));
        api.putAccount(cid, account(aid, "PLN", "0"));

        mvc.perform(get("/customers/{cid}/accounts/{aid}/delete", cid, aid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/delete"))
                .andExpect(model().attribute("canDelete", true));

        api.putAccount(cid, account(aid, "PLN", "5"));
        mvc.perform(get("/customers/{cid}/accounts/{aid}/delete", cid, aid))
                .andExpect(status().isOk())
                .andExpect(model().attribute("canDelete", false));
    }

    @Test @DisplayName("POST delete: blocks when balance != 0; success when 0")
    void doDelete() throws Exception {
        long cid = 1L, aid = 2L;

        api.putAccount(cid, account(aid, "PLN", "1"));
        mvc.perform(post("/customers/{cid}/accounts/{aid}/delete", cid, aid))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid))
                .andExpect(flash().attribute("error",
                        "Balance must be 0 before deleting the account."));
        assert !api.deleteCalled;

        api.putAccount(cid, account(aid, "PLN", "0"));
        mvc.perform(post("/customers/{cid}/accounts/{aid}/delete", cid, aid))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid))
                .andExpect(flash().attribute("message", "Account deleted."));
        assert api.deleteCalled && api.deletedCustomerId == cid && api.deletedAccountId == aid;
    }

    @Test @DisplayName("Deposit: GET form; POST ok; POST error")
    void deposit() throws Exception {
        long cid = 1L, aid = 2L;
        api.customers.put(cid, customer(cid, "Y"));
        api.putAccount(cid, account(aid, "PLN", "10"));

        mvc.perform(get("/customers/{cid}/accounts/{aid}/deposit", cid, aid))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/deposit"));

        mvc.perform(post("/customers/{cid}/accounts/{aid}/deposit", cid, aid)
                        .param("amount", "5")
                        .param("description", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + aid))
                .andExpect(flash().attribute("message", "Deposit successful."));

        api.depositThrows = new RuntimeException("boom");
        mvc.perform(post("/customers/{cid}/accounts/{aid}/deposit", cid, aid)
                        .param("amount", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + aid + "/deposit"))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.startsWith("Deposit failed:")));
    }

    @Test @DisplayName("Withdraw: ok and error")
    void withdraw() throws Exception {
        long cid = 1L, aid = 2L;

        mvc.perform(post("/customers/{cid}/accounts/{aid}/withdraw", cid, aid)
                        .param("amount", "3")
                        .param("description", "w"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + aid))
                .andExpect(flash().attribute("message", "Withdraw successful."));

        api.withdrawThrows = new RuntimeException("nope");
        mvc.perform(post("/customers/{cid}/accounts/{aid}/withdraw", cid, aid)
                        .param("amount", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + aid + "/withdraw"))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.startsWith("Withdraw failed:")));
    }

    @Test @DisplayName("GET transfer: targets — own accounts same currency, no self")
    void transferForm_targets() throws Exception {
        long cid = 1L, fromId = 10L;
        var from = account(fromId, "PLN", "100");
        var a1 = account(11L, "PLN", "0");
        var a2 = account(12L, "USD", "0");
        var a3 = account(fromId, "PLN", "0");

        api.customers.put(cid, customer(cid, "Z"));
        api.putAccount(cid, from);
        api.accountsByCustomer.put(cid, List.of(from, a1, a2, a3));

        mvc.perform(get("/customers/{cid}/accounts/{aid}/transfer", cid, fromId))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/transfer"))
                .andExpect(model().attribute("targets", hasSize(1)))
                .andExpect(model().attribute("hasTargets", true));
    }

    @Test @DisplayName("POST transfer: must choose exactly one destination")
    void transfer_badSelection() throws Exception {
        long cid = 1L, fromId = 10L;

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId + "/transfer"))
                .andExpect(flash().attribute("error", "Choose exactly one destination."));

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("toAccountId", "11")
                        .param("externalAccountId", "22")
                        .param("amount", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId + "/transfer"))
                .andExpect(flash().attribute("error", "Choose exactly one destination."));
    }

    @Test @DisplayName("POST transfer (internal): currency mismatch -> block")
    void transfer_internalCurrencyMismatch() throws Exception {
        long cid = 1L, fromId = 10L, toId = 11L;
        api.putAccount(cid, account(fromId, "PLN", "100"));
        api.putAccount(cid, account(toId, "USD", "0"));

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("toAccountId", String.valueOf(toId))
                        .param("amount", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId + "/transfer"))
                .andExpect(flash().attribute("error", "Currencies must match (PLN)."));

        // ensure transfer not executed
        assert api.lastTransfer == null;
    }

    @Test @DisplayName("POST transfer (external): currency mismatch using public GET -> block")
    void transfer_externalCurrencyMismatch() throws Exception {
        long cid = 1L, fromId = 10L, extId = 999L;
        api.putAccount(cid, account(fromId, "PLN", "100"));
        api.accountByAnyId.put(extId, account(extId, "USD", "0"));

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("externalAccountId", String.valueOf(extId))
                        .param("amount", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId + "/transfer"))
                .andExpect(flash().attribute("error", "Currencies must match (PLN)."));

        assert api.lastTransfer == null;
    }

    @Test @DisplayName("POST transfer: success")
    void transfer_ok() throws Exception {
        long cid = 1L, fromId = 10L, toId = 11L;
        api.putAccount(cid, account(fromId, "PLN", "100"));
        api.putAccount(cid, account(toId, "PLN", "0"));

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("toAccountId", String.valueOf(toId))
                        .param("amount", "5")
                        .param("description", "note"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId))
                .andExpect(flash().attribute("message", "Transfer successful."));

        // assert that fake captured the call
        var args = api.lastTransfer;
        assert args != null;
        assert args.cid == cid && args.fromId == fromId && args.toId == toId;
        assert new BigDecimal("5").compareTo(args.amount) == 0;
        assert "note".equals(args.description);
    }

    @Test @DisplayName("POST transfer: error -> back to form with flash")
    void transfer_error() throws Exception {
        long cid = 1L, fromId = 10L, toId = 11L;
        api.putAccount(cid, account(fromId, "PLN", "100"));
        api.putAccount(cid, account(toId, "PLN", "0"));
        api.transferThrows = new RuntimeException("fail");

        mvc.perform(post("/customers/{cid}/transfer", cid)
                        .param("fromAccountId", String.valueOf(fromId))
                        .param("toAccountId", String.valueOf(toId))
                        .param("amount", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers/" + cid + "/accounts/" + fromId + "/transfer"))
                .andExpect(flash().attribute("error",
                        org.hamcrest.Matchers.startsWith("Transfer failed:")));

        assert api.lastTransfer == null; // не записали из-за ошибки
    }

    // =======================================================================
    //                                Fake API
    // =======================================================================
    static class FakeApi extends BankApiClient {
        FakeApi() { super(new RestTemplate()); }

        // storage/config
        Map<Long, CustomerDto> customers = new HashMap<>();
        Map<Long, List<AccountDto>> accountsByCustomer = new HashMap<>();
        Map<Long, Map<Long, AccountDto>> customerAccounts = new HashMap<>();
        Map<String, List<TransactionDto>> transactions = new HashMap<>();
        Map<Long, AccountDto> accountByAnyId = new HashMap<>();

        RuntimeException transactionsThrow;
        RuntimeException createThrows;
        RuntimeException depositThrows;
        RuntimeException withdrawThrows;
        RuntimeException transferThrows;

        boolean createCalled = false;
        boolean deleteCalled = false;
        Long deletedCustomerId, deletedAccountId;

        static class TransferArgs {
            long cid, fromId, toId; BigDecimal amount; String description;
        }
        TransferArgs lastTransfer;

        static String key(Long cid, Long aid) { return cid + ":" + aid; }

        void putAccount(long customerId, AccountDto a) {
            customerAccounts.computeIfAbsent(customerId, k -> new HashMap<>()).put(a.getId(), a);
        }

        // ---------- overrides used by controller ----------

        @Override public CustomerDto getCustomer(Long id) {
            return customers.get(id);
        }

        @Override public List<AccountDto> getAccountsByCustomer(Long customerId) {
            return accountsByCustomer.getOrDefault(customerId, List.of());
        }

        @Override public AccountDto getAccount(Long customerId, Long accountId) {
            var map = customerAccounts.get(customerId);
            return map == null ? null : map.get(accountId);
        }

        @Override public List<TransactionDto> getAccountTransactions(Long customerId, Long accountId) {
            if (transactionsThrow != null) throw transactionsThrow;
            return transactions.getOrDefault(key(customerId, accountId), List.of());
        }

        @Override public AccountDto createAccount(Long customerId, String number, String currency, BigDecimal balance) {
            createCalled = true;
            if (createThrows != null) throw createThrows;
            return createResult;
        }
        AccountDto createResult;

        @Override public void deleteAccount(Long customerId, Long accountId) {
            deleteCalled = true; deletedCustomerId = customerId; deletedAccountId = accountId;
        }

        @Override public TransactionDto deposit(Long customerId, Long accountId, BigDecimal amount, String description) {
            if (depositThrows != null) throw depositThrows;
            return new TransactionDto();
        }

        @Override public TransactionDto withdraw(Long customerId, Long accountId, BigDecimal amount, String description) {
            if (withdrawThrows != null) throw withdrawThrows;
            return new TransactionDto();
        }

        @Override public TransactionDto transfer(Long customerId, Long fromAccountId, Long toAccountId, BigDecimal amount, String description) {
            if (transferThrows != null) throw transferThrows;
            lastTransfer = new TransferArgs();
            lastTransfer.cid = customerId;
            lastTransfer.fromId = fromAccountId;
            lastTransfer.toId = toAccountId;
            lastTransfer.amount = amount;
            lastTransfer.description = description;
            return new TransactionDto();
        }

        @Override public AccountDto getAccountByAnyId(Long accountId) {
            return accountByAnyId.get(accountId);
        }
    }
}
