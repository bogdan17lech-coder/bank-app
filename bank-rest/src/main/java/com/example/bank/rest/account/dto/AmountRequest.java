package com.example.bank.rest.account.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AmountRequest {
    @NotNull
    private BigDecimal amount;
    private String description;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
