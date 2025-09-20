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

    // Redirect root accounts to customers list
    @GetMapping("/accounts")
    public String accountsEntry() {
        return "redirect:/customers";
    }

    // List accounts for a customer
    @GetMapping("/customers/{customerId}/accounts")
    public String list(@PathVariable Long customerId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("accounts", api.getAccountsByCustomer(customerId));
        return "accounts/list";
    }

    // View single account + recent transactions (empty list if API fails)
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

    // Show "new account" form
    @GetMapping("/customers/{customerId}/accounts/new")
    public String newForm(@PathVariable Long customerId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        return "accounts/new";
    }

    // Create account (validates non-negative initial balance)
    @PostMapping("/customers/{customerId}/accounts")
    public String create(@PathVariable Long customerId,
                         @RequestParam String number,
                         @RequestParam String currency,
                         @RequestParam BigDecimal balance,
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

    // Confirm delete page (allowed only when balance == 0)
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

    // Perform delete (guard if balance != 0)
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

    // Deposit form
    @GetMapping("/customers/{customerId}/accounts/{accountId}/deposit")
    public String depositForm(@PathVariable Long customerId, @PathVariable Long accountId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("account", api.getAccount(customerId, accountId));
        return "accounts/deposit";
    }

    // Do deposit
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

    // Withdraw form
    @GetMapping("/customers/{customerId}/accounts/{accountId}/withdraw")
    public String withdrawForm(@PathVariable Long customerId, @PathVariable Long accountId, Model model) {
        model.addAttribute("customer", api.getCustomer(customerId));
        model.addAttribute("account", api.getAccount(customerId, accountId));
        return "accounts/withdraw";
    }

    // Do withdraw
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

    // Transfer form (pre-fills same-currency targets, excludes self)
    @GetMapping("/customers/{customerId}/accounts/{accountId}/transfer")
    public String transferForm(@PathVariable Long customerId,
                               @PathVariable Long accountId,
                               Model model) {
        var customer = api.getCustomer(customerId);
        var from = api.getAccount(customerId, accountId);
        var all = api.getAccountsByCustomer(customerId);

        // Build list of own target accounts with same currency (exclude self)
        var sameCurrencyTargets = new java.util.ArrayList<>(all);
        sameCurrencyTargets.removeIf(a ->
                a.getId().equals(from.getId()) ||
                        (a.getCurrency() != null && !a.getCurrency().equals(from.getCurrency()))
        );

        model.addAttribute("customer", customer);
        model.addAttribute("from", from);
        model.addAttribute("accounts", all);                // for "from" select
        model.addAttribute("targets", sameCurrencyTargets); // own "to" options
        model.addAttribute("hasTargets", !sameCurrencyTargets.isEmpty());
        return "accounts/transfer";
    }

    // Unified transfer handler (own account or external by ID)
    @PostMapping("/customers/{customerId}/transfer")
    public String doTransferAny(@PathVariable Long customerId,
                                @RequestParam Long fromAccountId,
                                @RequestParam(required = false) Long toAccountId,       // own account
                                @RequestParam(required = false) Long externalAccountId, // external account
                                @RequestParam java.math.BigDecimal amount,
                                @RequestParam(required = false) String description,
                                org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            // Exactly one destination must be chosen
            if ((toAccountId == null) == (externalAccountId == null)) {
                ra.addFlashAttribute("error", "Choose exactly one destination.");
                return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
            }
            var from = api.getAccount(customerId, fromAccountId);
            Long destId = (toAccountId != null) ? toAccountId : externalAccountId;

            // Currency check for own transfers
            if (toAccountId != null) {
                var to = api.getAccount(customerId, toAccountId);
                if (from.getCurrency() != null && to.getCurrency() != null
                        && !from.getCurrency().equals(to.getCurrency())) {
                    ra.addFlashAttribute("error", "Currencies must match (" + from.getCurrency() + ").");
                    return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
                }
            }
            // Optional currency check for external (if public endpoint exists)
            try {
                var publicTo = api.getAccountByAnyId(destId);
                if (publicTo != null && publicTo.getCurrency() != null
                        && from.getCurrency() != null
                        && !from.getCurrency().equals(publicTo.getCurrency())) {
                    ra.addFlashAttribute("error", "Currencies must match (" + from.getCurrency() + ").");
                    return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
                }
            } catch (Exception ignore) { /* no public endpoint – backend will validate */ }

            api.transfer(customerId, fromAccountId, destId, amount, description);
            ra.addFlashAttribute("message", "Transfer successful.");
            return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId;
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Transfer failed: " + ex.getMessage());
            return "redirect:/customers/" + customerId + "/accounts/" + fromAccountId + "/transfer";
        }
    }
}
