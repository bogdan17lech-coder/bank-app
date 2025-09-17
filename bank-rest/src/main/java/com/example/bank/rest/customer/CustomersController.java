package com.example.bank.rest.customer;

import com.example.bank.core.dto.CustomerDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomersController {

    private final CustomerService service;

    public CustomersController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerDto> all() {
        return service.all();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDto create(@RequestBody @Valid CustomerDto dto) {
        return service.create(dto);
    }

    @GetMapping("/{id}")
    public CustomerDto byId(@PathVariable long id) {
        return service.byId(id);
    }

    @PutMapping("/{id}")
    public CustomerDto update(@PathVariable long id, @RequestBody @Valid CustomerDto dto) {
        return service.update(id, dto); // полный апдейт
    }

    @GetMapping("/search")
    public List<CustomerDto> search(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return service.search(q, page, size);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
