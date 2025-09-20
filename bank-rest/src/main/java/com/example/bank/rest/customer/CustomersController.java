package com.example.bank.rest.customer;

import com.example.bank.core.dto.CustomerDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** JSON API for customers under /api/customers */
@RestController
@RequestMapping("/api/customers")
public class CustomersController {

    private final CustomerService service;

    public CustomersController(CustomerService service) {
        this.service = service;
    }

    // List all (simple demo endpoint)
    @GetMapping
    public List<CustomerDto> all() {
        return service.all();
    }

    // Create (validates DTO)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@RequestBody @Valid CustomerDto dto) {
        return service.create(dto);
    }

    // Get by id (404 handled in service)
    @GetMapping("/{id}")
    public CustomerDto byId(@PathVariable long id) {
        return service.byId(id);
    }

    // Full update (PUT)
    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable long id, @RequestBody @Valid CustomerDto dto) {
        return service.update(id, dto);
    }

    // Search + paging (?q=&page=&size=)
    @GetMapping("/search")
    public List<CustomerDto> search(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return service.search(q, page, size);
    }

    // Delete (204 on success)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
