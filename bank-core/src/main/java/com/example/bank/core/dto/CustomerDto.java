package com.example.bank.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Simple DTO for customer data (used in API layer)
public class CustomerDto {
    private Long id;

    // Required first name
    @NotBlank(message = "firstName is required")
    private String firstName;

    // Optional last name
    private String lastName;

    // Required email, must be valid format
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String email;

    // No-args ctor for frameworks (Jackson/validation)
    public CustomerDto() {}

    // Convenience ctor for mapping/tests
    public CustomerDto(Long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    // Getters/Setters (no extra logic)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
