package com.example.bank.mvc.dto;

import java.math.BigDecimal;

// DTO used on MVC side (view layer) for showing account data
public class AccountDto {
    private Long id;
    private Long customerId;   // Link to customer
    private String number;     // Account number
    private BigDecimal balance;
    private String currency;   // ISO code (e.g. "PLN", "USD")

    // Getters/Setters only, no extra logic
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
