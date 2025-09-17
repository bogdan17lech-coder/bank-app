package com.example.bank.rest.account;

import com.example.bank.rest.account.dto.AccountDto;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class PublicAccountsController {

    private final AccountService service;

    public PublicAccountsController(AccountService service) { this.service = service; }

    @GetMapping("/{accountId}")
    public AccountDto getPublic(@PathVariable long accountId) {
        return service.getPublic(accountId);
    }
}
