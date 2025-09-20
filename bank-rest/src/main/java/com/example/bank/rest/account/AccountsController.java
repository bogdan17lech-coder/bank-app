package com.example.bank.rest.account;

import com.example.bank.rest.account.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for customer accounts (nested under /api/customers/{customerId}).
 * Endpoints are simple CRUD + money operations.
 */
@RestController
@RequestMapping("/api/customers/{customerId}/accounts")
public class AccountsController {

    private final AccountService service;

    public AccountsController(AccountService service) { this.service = service; }

    // List all accounts of a customer
    @GetMapping
    public List<AccountDto> list(@PathVariable long customerId) {
        return service.listByCustomer(customerId);
    }

    // Create account for customer
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto create(@PathVariable long customerId, @Valid @RequestBody NewAccountRequest req) {
        return service.create(customerId, req);
    }

    // Get one account (ownership checked in service)
    @GetMapping("/{accountId}")
    public AccountDto get(@PathVariable long customerId, @PathVariable long accountId) {
        return service.getByCustomer(customerId, accountId);
    }

    // Delete account (allowed when balance == 0)
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long customerId, @PathVariable long accountId) {
        service.delete(customerId, accountId);
    }

    // Deposit money
    @PostMapping("/{accountId}/deposit")
    public TransactionDto deposit(@PathVariable long customerId,
                                  @PathVariable long accountId,
                                  @Valid @RequestBody AmountRequest req) {
        return service.deposit(customerId, accountId, req);
    }

    // Withdraw money
    @PostMapping("/{accountId}/withdraw")
    public TransactionDto withdraw(@PathVariable long customerId,
                                   @PathVariable long accountId,
                                   @Valid @RequestBody AmountRequest req) {
        return service.withdraw(customerId, accountId, req);
    }

    // Transfer from one account to another (toAccountId in body)
    @PostMapping("/{fromAccountId}/transfer")
    public TransactionDto transfer(@PathVariable long customerId,
                                   @PathVariable long fromAccountId,
                                   @Valid @RequestBody TransferRequest req) {
        return service.transfer(customerId, fromAccountId, req);
    }

    // Expose transactions for MVC (read-only)
    @GetMapping("/{accountId}/transactions")
    public List<TransactionDto> transactions(@PathVariable long customerId,
                                             @PathVariable long accountId) {
        return service.listTransactions(customerId, accountId);
    }
}
