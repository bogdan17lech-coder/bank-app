package com.example.bank.core.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

// Simple DTO wrapper for a money value in requests (e.g., deposit/withdraw amount)
public class AmountDto {

    // Required monetary amount (BigDecimal to avoid float rounding issues)
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    // No-args ctor for Jackson/validation
    public AmountDto() { }

    // Convenience ctor for tests/manual usage
    public AmountDto(BigDecimal amount) { this.amount = amount; }

    // Getters/Setters (no extra logic)
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
