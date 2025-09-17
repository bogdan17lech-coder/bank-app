package com.example.bank.rest.account.dto;

import java.math.BigDecimal;

public class AccountDto {
    private Long id;
    private Long customerId;
    private String number;
    private String currency;
    private BigDecimal balance;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
