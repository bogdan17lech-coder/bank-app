package com.example.bank.rest.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    // все счета клиента
    List<AccountEntity> findByCustomerId(Long customerId);

    // конкретный счёт, проверяя владельца
    Optional<AccountEntity> findByIdAndCustomerId(Long id, Long customerId);

    // по номеру (удобно для проверок уникальности/поиска)
    Optional<AccountEntity> findByNumber(String number);
}
