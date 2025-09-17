package com.example.bank.rest.account;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_customer", columnList = "customerId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_accounts_number", columnNames = "number")
})
public class AccountEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 64)
    private String number;

    @Column(nullable = false, length = 8)
    private String currency; // "USD","PLN","EUR"...

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    // getters/setters
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
