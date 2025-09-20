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

// Service for customer CRUD + simple search (REST side)
@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    // Return all customers (no paging) — demo endpoint
    public List<CustomerDto> all() {
        List<CustomerEntity> entities = repo.findAll();
        List<CustomerDto> result = new ArrayList<>();
        for (CustomerEntity e : entities) {
            result.add(toDto(e));
        }
        return result;
    }

    // Create new customer (checks email uniqueness)
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

    // Get by id or 404
    public CustomerDto byId(long id) {
        CustomerEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));
        return toDto(e);
    }

    // Full update (PUT). If email changes → verify uniqueness on other ids.
    public CustomerDto update(long id, CustomerDto dto) {
        CustomerEntity e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));

        if (!dto.getEmail().equals(e.getEmail())
                && repo.existsByEmailAndIdNot(dto.getEmail(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        e.setFirstName(dto.getFirstName());
        e.setLastName(dto.getLastName()); // lastName may be null/blank — it's ok
        e.setEmail(dto.getEmail());

        return toDto(repo.save(e));
    }

    // Search by name with paging (q optional)
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
        return result; // junior version: return only list (no metadata)
    }

    // Delete by id (404 if missing)
    public void delete(long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found");
        }
        repo.deleteById(id);
    }

    // Simple mapper
    private static CustomerDto toDto(CustomerEntity e) {
        return new CustomerDto(e.getId(), e.getFirstName(), e.getLastName(), e.getEmail());
    }
}
