package com.example.bank.rest.account.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionDto {
    private Long accountId;
    private String type;          // DEPOSIT / WITHDRAW / TRANSFER_OUT / TRANSFER_IN
    private BigDecimal amount;
    private String description;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TransactionDto() { }
    public TransactionDto(Long accountId, String type, BigDecimal amount, String description) {
        this.accountId = accountId; this.type = type; this.amount = amount; this.description = description;
    }

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
