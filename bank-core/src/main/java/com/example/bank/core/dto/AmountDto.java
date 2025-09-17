package com.example.bank.core.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AmountDto {
    @NotNull(message = "amount is required")
    private BigDecimal amount;

    public AmountDto() { }
    public AmountDto(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
