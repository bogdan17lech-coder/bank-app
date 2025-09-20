package com.example.bank.mvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Entry point for the MVC app (component scan + auto-config)
@SpringBootApplication
public class BankMvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankMvcApplication.class, args);
    }
}
