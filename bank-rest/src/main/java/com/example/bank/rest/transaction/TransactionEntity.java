package com.example.bank.rest.transaction;

import com.example.bank.rest.account.AccountEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

// JPA entity for account transactions (REST side)
@Entity
@Table(name = "account_transactions",
        indexes = {
                @Index(name = "idx_trx_account", columnList = "account_id"),
                @Index(name = "idx_trx_created", columnList = "created_at")
        })
public class TransactionEntity {

    // PK (auto-increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Owning account (lazy by default)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    // DEPOSIT / WITHDRAW / TRANSFER_IN / TRANSFER_OUT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    // Operation amount (DECIMAL 19,2)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Balance snapshot after this op (optional)
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    // Optional note
    @Column(length = 255)
    private String description;

    // Created timestamp (set on insert)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Auto-fill createdAt if not provided
    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public TransactionEntity() {}

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AccountEntity getAccount() { return account; }
    public void setAccount(AccountEntity account) { this.account = account; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
