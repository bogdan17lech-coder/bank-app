package com.example.bank.core.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionDto {
    private Long id;
    private String type;           // DEPOSIT / WITHDRAW
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private Instant createdAt;

    public TransactionDto() {}

    public TransactionDto(Long id, String type, BigDecimal amount, BigDecimal balanceAfter, Instant createdAt) {
        this.id = id; this.type = type; this.amount = amount; this.balanceAfter = balanceAfter; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
