package com.example.bank.rest.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // <-- нужен для проверки уникальности email при update(id)
    boolean existsByEmailAndIdNot(String email, Long id);

    // для постраничного списка
    Page<CustomerEntity> findAll(Pageable pageable);

    // для поиска по q (то, что зовётся из CustomerService.search)
    Page<CustomerEntity> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstNamePart, String lastNamePart, Pageable pageable
    );
}
