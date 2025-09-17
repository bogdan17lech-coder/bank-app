package com.example.bank.mvc.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionDto {
    private Long id;
    private Long accountId;
    private String type; // DEPOSIT / WITHDRAW / TRANSFER
    private BigDecimal amount;
    private String description;
    private OffsetDateTime createdAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
