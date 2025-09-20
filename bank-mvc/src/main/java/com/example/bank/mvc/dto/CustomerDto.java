package com.example.bank.mvc.dto;

// DTO used on MVC side (view layer) for showing customer data
public class CustomerDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;

    // fullName is calculated in service (not persisted in DB)
    private String fullName;

    // Getters/Setters only, no extra logic
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
