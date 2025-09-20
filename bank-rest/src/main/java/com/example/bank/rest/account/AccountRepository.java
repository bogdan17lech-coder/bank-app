package com.example.bank.rest.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Spring Data JPA repo for accounts (REST side)
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    // All accounts of a customer
    List<AccountEntity> findByCustomerId(Long customerId);

    // One account by id, but also verify owner
    Optional<AccountEntity> findByIdAndCustomerId(Long id, Long customerId);

    // Lookup by unique account number (for search/uniqueness checks)
    Optional<AccountEntity> findByNumber(String number);
}
