package com.example.bank.rest.customer;

import com.example.bank.core.dto.CustomerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public List<CustomerDto> all() {
        List<CustomerEntity> entities = repo.findAll();
        List<CustomerDto> result = new ArrayList<>();
        for (CustomerEntity e : entities) {
            result.add(toDto(e));
        }
        return result;
    }

    public CustomerDto create(CustomerDto dto) {
        if (repo.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }
        CustomerEntity e = new CustomerEntity();
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());
        e.setEmail(dto.getEmail());
        return toDto(repo.save(e));
    }

    public CustomerDto byId(long id) {
        CustomerEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));
        return toDto(e);
    }

    public CustomerDto update(long id, CustomerDto dto) {
        CustomerEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));

        // если email меняется — проверим уникальность у других
        if (!dto.getEmail().equals(e.getEmail())
                && repo.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        // перезаписываем все поля
        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName());          // lastName может быть null/пустым — это ок
        e.setEmail(dto.getEmail());

        return toDto(repo.save(e));
    }

    public List<CustomerDto> search(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<CustomerEntity> p;
        if (q == null || q.isBlank()) {
            p = repo.findAll(pageable);
        } else {
            p = repo.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q, pageable);
        }

        List<CustomerDto> result = new ArrayList<>();
        for (CustomerEntity e : p.getContent()) {
            result.add(toDto(e));
        }
        return result; // пока без метаданных — чисто джун-версия
    }


    public void delete(long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found");
        }
        repo.deleteById(id);
    }

    private static CustomerDto toDto(CustomerEntity e) {
        return new CustomerDto(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail());
    }
}
