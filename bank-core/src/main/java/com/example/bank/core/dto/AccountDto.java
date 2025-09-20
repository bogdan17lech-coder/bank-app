package com.example.bank.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Simple DTO for account data used in API layer
public class AccountDto {
    private Long id;

    // Required account number (e.g., IBAN or internal)
    @NotBlank(message = "number is required")
    private String number;

    // Required ISO currency code (e.g., "PLN", "USD")
    @NotBlank(message = "currency is required")
    private String currency;

    // Required monetary amount (BigDecimal to avoid float rounding)
    @NotNull(message = "balance is required")
    private BigDecimal balance;

    // No-args ctor for frameworks (Jackson/validation)
    public AccountDto() { }

    // Convenience ctor for manual mapping/tests
    public AccountDto(Long id, String number, String currency, BigDecimal balance) {
        this.id = id; this.number = number; this.currency = currency; this.balance = balance;
    }

    // Getters/Setters (no extra logic)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
