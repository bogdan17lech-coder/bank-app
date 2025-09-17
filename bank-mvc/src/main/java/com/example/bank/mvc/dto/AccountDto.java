package com.example.bank.mvc.dto;

import java.math.BigDecimal;

public class AccountDto {
    private Long id;
    private Long customerId;
    private String number;
    private BigDecimal balance;
    private String currency;

    // getters/setters
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
