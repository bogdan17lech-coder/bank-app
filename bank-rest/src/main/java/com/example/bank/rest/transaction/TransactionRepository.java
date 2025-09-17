package com.example.bank.rest.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    // для истории операций (верхние 100)
    List<TransactionEntity> findTop100ByAccount_IdOrderByCreatedAtDesc(Long accountId);

    // если нужно без лимита:
    List<TransactionEntity> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
}
