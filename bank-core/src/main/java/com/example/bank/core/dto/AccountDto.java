package com.example.bank.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AccountDto {
    private Long id;

    @NotBlank(message = "number is required")
    private String number;

    @NotBlank(message = "currency is required")
    private String currency;

    @NotNull(message = "balance is required")
    private BigDecimal balance;

    public AccountDto() { }

    public AccountDto(Long id, String number, String currency, BigDecimal balance) {
        this.id = id; this.number = number; this.currency = currency; this.balance = balance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
