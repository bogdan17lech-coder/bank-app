package com.example.bank.rest.account;

import com.example.bank.rest.account.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/accounts")
public class AccountsController {

    private final AccountService service;

    public AccountsController(AccountService service) { this.service = service; }

    @GetMapping
    public List<AccountDto> list(@PathVariable long customerId) {
        return service.listByCustomer(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto create(@PathVariable long customerId, @Valid @RequestBody NewAccountRequest req) {
        return service.create(customerId, req);
    }

    @GetMapping("/{accountId}")
    public AccountDto get(@PathVariable long customerId, @PathVariable long accountId) {
        return service.getByCustomer(customerId, accountId);
    }

    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long customerId, @PathVariable long accountId) {
        service.delete(customerId, accountId);
    }

    @PostMapping("/{accountId}/deposit")
    public TransactionDto deposit(@PathVariable long customerId,
                                  @PathVariable long accountId,
                                  @Valid @RequestBody AmountRequest req) {
        return service.deposit(customerId, accountId, req);
    }

    @PostMapping("/{accountId}/withdraw")
    public TransactionDto withdraw(@PathVariable long customerId,
                                   @PathVariable long accountId,
                                   @Valid @RequestBody AmountRequest req) {
        return service.withdraw(customerId, accountId, req);
    }

    // этот путь дергает MVC при переводе
    @PostMapping("/{fromAccountId}/transfer")
    public TransactionDto transfer(@PathVariable long customerId,
                                   @PathVariable long fromAccountId,
                                   @Valid @RequestBody TransferRequest req) {
        return service.transfer(customerId, fromAccountId, req);
    }

    // ВОТ ЭТО — чтобы MVC видел транзакции
    @GetMapping("/{accountId}/transactions")
    public List<TransactionDto> transactions(@PathVariable long customerId,
                                             @PathVariable long accountId) {
        return service.listTransactions(customerId, accountId);
    }
}
