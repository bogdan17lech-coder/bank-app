package com.example.bank.rest.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Spring Data JPA repo for transactions
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // Last 100 transactions for account (newest first)
    List<TransactionEntity> findTop100ByAccount_IdOrderByCreatedAtDesc(Long accountId);

    // All transactions for account (newest first)
    List<TransactionEntity> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
}
