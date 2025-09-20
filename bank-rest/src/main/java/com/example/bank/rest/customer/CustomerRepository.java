package com.example.bank.rest.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data JPA repo for customers (REST side)
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    // Find by unique email
    Optional<CustomerEntity> findByEmail(String email);

    // Check if email already exists
    boolean existsByEmail(String email);

    // For update: email exists but belongs to another id
    boolean existsByEmailAndIdNot(String email, Long id);

    // Paged list
    Page<CustomerEntity> findAll(Pageable pageable);

    // Search by first or last name (case-insensitive), paged
    Page<CustomerEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstNamePart, String lastNamePart, Pageable pageable
    );
}
