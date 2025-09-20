package com.example.bank.rest.account;

import com.example.bank.rest.account.dto.AccountDto;
import org.springframework.web.bind.annotation.*;

/**
 * Public read-only endpoint to fetch account info by id.
 * (Used by MVC for optional pre-checks like currency.)
 */
@RestController
@RequestMapping("/api/accounts")
public class PublicAccountsController {

    private final AccountService service;

    public PublicAccountsController(AccountService service) { this.service = service; }

    // GET /api/accounts/{id} → basic account data
    @GetMapping("/{accountId}")
    public AccountDto getPublic(@PathVariable long accountId) {
        return service.getPublic(accountId);
    }
}
