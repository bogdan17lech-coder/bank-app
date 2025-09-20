package com.example.bank.mvc.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Simple home page for MVC app
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "Bank MVC");
        model.addAttribute("message", "Bank MVC is up ✅");
        return "index"; // returns Thymeleaf template "index.html"
    }
}
