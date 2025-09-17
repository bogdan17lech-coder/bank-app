package com.example.bank.rest.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class NewAccountRequest {
    @NotBlank
    private String number;
    @NotBlank
    private String currency;
    @NotNull
    private BigDecimal balance;

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
