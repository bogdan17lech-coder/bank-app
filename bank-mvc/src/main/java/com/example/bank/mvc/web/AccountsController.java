package com.example.bank.mvc.web;

import com.example.bank.mvc.dto.AccountDto;
import com.example.bank.mvc.service.BankApiClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;

@Controller
public class AccountsController {

    private final BankApiClient api;

    public AccountsController(BankApiClient api) {
        this.api = api;
    }

    @GetMapping("/accounts")
    public String accountsEntry() {
        return "redirect:/customers";
    }

    @GetMapping("/customers/{customerId}/accounts")
    public String list(@PathVariable Long customerId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("accounts", api.getAccountsByCustomer(customerId));
        return "accounts/list";
    }

    @GetMapping("/customers/{customerId}/accounts/{accountId}")
    public String view(@PathVariable Long customerId,
                       @PathVariable Long accountId,
                       Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("account", api.getAccount(customerId, accountId));
        try {
            model.addAttribute("transactions", api.getAccountTransactions(customerId, accountId));
        } catch (Exception e) {
            model.addAttribute("transactions", java.util.List.of());
        }
        return "accounts/view";
    }


    // ----- Open account -----
    @GetMapping("/customers/{customerId}/accounts/new")
    public String newForm(@PathVariable Long customerId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        return "accounts/new";
    }


    @PostMapping("/customers/{customerId}/accounts")
    public String create(@PathVariable Long customerId,
                         @RequestParam String number,
                         @RequestParam String currency,
                         @RequestParam BigDecimal balance,   // <-- NEW
                         RedirectAttributes ra) {
        try {
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                ra.addFlashAttribute("error", "Initial balance cannot be negative.");
                return "redirect:/customers/" + customerId + "/accounts/new";
            }
            var acc = api.createAccount(customerId, number, currency, balance);
            ra.addFlashAttribute("message", "Account created.");
            return "redirect:/customers/" + customerId + "/accounts/" + acc.getId();
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to create account: " + ex.getMessage());
            return "redirect:/customers/" + customerId;
        }
    }

    // ---------- Delete account ----------
    @GetMapping("/customers/{customerId}/accounts/{accountId}/delete")
    public String confirmDelete(@PathVariable Long customerId,
                                @PathVariable Long accountId,
                                Model model) {
        var customer = api.getCustomer(customerId);
        var account = api.getAccount(customerId, accountId);
        boolean canDelete = account.getBalance() == null
                || account.getBalance().compareTo(BigDecimal.ZERO) == 0;

        model.addAttribute("customer", customer);
        model.addAttribute("account", account);
        model.addAttribute("canDelete", canDelete);
        return "accounts/delete";
    }

    @PostMapping("/customers/{customerId}/accounts/{accountId}/delete")
    public String doDelete(@PathVariable Long customerId,
                           @PathVariable Long accountId,
                           RedirectAttributes ra) {
        try {
            var acc = api.getAccount(customerId, accountId);
            if (acc.getBalance() != null && acc.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                ra.addFlashAttribute("error", "Balance must be 0 before deleting the account.");
                return "redirect:/customers/" + customerId;
            }
            api.deleteAccount(customerId, accountId);
            ra.addFlashAttribute("message", "Account deleted.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to delete account: " + ex.getMessage());
        }
        return "redirect:/customers/" + customerId;
    }

    // ---------- Deposit ----------
    @GetMapping("/customers/{customerId}/accounts/{accountId}/deposit")
    public String depositForm(@PathVariable Long customerId, @PathVariable Long accountId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("account", api.getAccount(customerId, accountId));
        return "accounts/deposit";
    }

    @PostMapping("/customers/{customerId}/accounts/{accountId}/deposit")
    public String doDeposit(@PathVariable Long customerId, @PathVariable Long accountId,
                            @RequestParam BigDecimal amount,
                            @RequestParam(required = false) String description,
                            RedirectAttributes ra) {
        try {
            api.deposit(customerId, accountId, amount, description);
            ra.addFlashAttribute("message", "Deposit successful.");
            return "redirect:/customers/" + customerId + "/accounts/" + accountId;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Deposit failed: " + ex.getMessage());
            return "redirect:/customers/" + customerId + "/accounts/" + accountId + "/deposit";
        }
    }

    // ---------- Withdraw ----------
    @GetMapping("/customers/{customerId}/accounts/{accountId}/withdraw")
    public String withdrawForm(@PathVariable Long customerId, @PathVariable Long accountId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("account", api.getAccount(customerId, accountId));
        return "accounts/withdraw";
    }

    @PostMapping("/customers/{customerId}/accounts/{accountId}/withdraw")
    public String doWithdraw(@PathVariable Long customerId, @PathVariable Long accountId,
                             @RequestParam BigDecimal amount,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        try {
            api.withdraw(customerId, accountId, amount, description);
            ra.addFlashAttribute("message", "Withdraw successful.");
            return "redirect:/customers/" + customerId + "/accounts/" + accountId;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Withdraw failed: " + ex.getMessage());
            return "redirect:/customers/" + customerId + "/accounts/" + accountId + "/withdraw";
        }
    }

    // открыть форму transfer (из строки счёта) — как было
    @GetMapping("/customers/{customerId}/accounts/{accountId}/transfer")
    public String transferForm(@PathVariable Long customerId,
                               @PathVariable Long accountId,
                               Model model) {
        var customer = api.getCustomer(customerId);
        var from = api.getAccount(customerId, accountId);
        var all = api.getAccountsByCustomer(customerId);

        // список собственных получателей той же валюты (без самого себя)
        var sameCurrencyTargets = new java.util.ArrayList<>(all);
        sameCurrencyTargets.removeIf(a ->
                a.getId().equals(from.getId()) ||
                        (a.getCurrency() != null && !a.getCurrency().equals(from.getCurrency()))
        );

        model.addAttribute("customer", customer);
        model.addAttribute("from", from);
        model.addAttribute("accounts", all);              // для селекта "from"
        model.addAttribute("targets", sameCurrencyTargets); // "to (my)"
        model.addAttribute("hasTargets", !sameCurrencyTargets.isEmpty());
        return "accounts/transfer";
    }

    // единый обработчик transfer (и на свои счета, и на чужой ID)
    @PostMapping("/customers/{customerId}/transfer")
    public String doTransferAny(@PathVariable Long customerId,
                                @RequestParam Long fromAccountId,
                                @RequestParam(required = false) Long toAccountId,       // мой счёт
                                @RequestParam(required = false) Long externalAccountId, // чужой счёт
                                @RequestParam java.math.BigDecimal amount,
                                @RequestParam(required = false) String description,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            if ((toAccountId == null) == (externalAccountId == null)) {
                ra.addFlashAttribute("error", "Choose exactly one destination.");
                return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
            }
            var from = api.getAccount(customerId, fromAccountId);

            Long destId = (toAccountId != null) ? toAccountId : externalAccountId;

            // ВАЛЮТА: если перевод на свой счёт — проверим строго
            if (toAccountId != null) {
                var to = api.getAccount(customerId, toAccountId);
                if (from.getCurrency() != null && to.getCurrency() != null
                        && !from.getCurrency().equals(to.getCurrency())) {
                    ra.addFlashAttribute("error", "Currencies must match (" + from.getCurrency() + ").");
                    return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
                }
            }
            // Внешний перевод: если у REST есть публичный GET /api/accounts/{id}, можно тоже сверить валюты.
            try {
                var publicTo = api.getAccountByAnyId(destId); // может бросить, если нет эндпойнта
                if (publicTo != null && publicTo.getCurrency() != null
                        && from.getCurrency() != null
                        && !from.getCurrency().equals(publicTo.getCurrency())) {
                    ra.addFlashAttribute("error", "Currencies must match (" + from.getCurrency() + ").");
                    return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
                }
            } catch (Exception ignore) { /* нет публичного эндпойнта — проверит REST */ }

            api.transfer(customerId, fromAccountId, destId, amount, description);
            ra.addFlashAttribute("message", "Transfer successful.");
            return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Transfer failed: " + ex.getMessage());
            return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
        }
    }
}