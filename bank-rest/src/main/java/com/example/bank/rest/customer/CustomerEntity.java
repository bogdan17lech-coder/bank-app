package com.example.bank.rest.customer;

import jakarta.persistence.*;

// JPA entity for customers (REST side)
@Entity
@Table(name = "customers")
public class CustomerEntity {

    // DB id (auto-increment)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Required first name (mapped to column "name")
    @Column(name = "name", nullable = false)
    private String firstName;

    // Optional last name
    @Column(name = "last_name")
    private String lastName;

    // Unique email (required)
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // No-args ctor for JPA
    public CustomerEntity() { }

    // Getters/Setters only, no extra logic
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
