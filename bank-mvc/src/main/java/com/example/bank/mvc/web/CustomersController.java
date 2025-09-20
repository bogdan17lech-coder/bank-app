package com.example.bank.mvc.web;

import com.example.bank.mvc.dto.CustomerDto;
import com.example.bank.mvc.service.BankApiClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CustomersController {

    private final BankApiClient api;

    public CustomersController(BankApiClient api) {
        this.api = api;
    }

    // List all customers (shows empty list + error banner on failure)
    @GetMapping("/customers")
    public String list(Model model) {
        try {
            model.addAttribute("customers", api.getCustomers());
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to load customers: " + ex.getMessage());
            model.addAttribute("customers", java.util.List.of());
        }
        return "customers/list";
    }

    // View one customer + their accounts
    @GetMapping("/customers/{id}")
    public String view(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("customer", api.getCustomer(id));
            model.addAttribute("accounts", api.getAccountsByCustomer(id)); // eager-load for page
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to load customer: " + ex.getMessage());
        }
        return "customers/view";
    }

    // Create form
    @GetMapping("/customers/new")
    public String newForm(Model model) {
        model.addAttribute("customer", new CustomerDto());
        return "customers/new";
    }

    // Create action (handles 409 for duplicate email)
    @PostMapping("/customers")
    public String create(@ModelAttribute("customer") CustomerDto form, Model model) {
        try {
            var created = api.createCustomer(form);
            if (created != null && created.getId() != null) {
                return "redirect:/customers/" + created.getId();
            }
            return "redirect:/customers";
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 409) {
                String msg = extractApiMessage(ex);
                model.addAttribute("emailError",
                        (msg == null || msg.isBlank()) ? "Email already exists." : msg);
                return "customers/new";
            }
            model.addAttribute("error", "Failed to create: " + ex.getStatusCode().value() +
                    (ex.getResponseBodyAsString() != null ? " : " + ex.getResponseBodyAsString() : ""));
            return "customers/new";
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to create: " + ex.getMessage());
            return "customers/new";
        }
    }

    // Edit form
    @GetMapping("/customers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("customer", api.getCustomer(id));
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to load customer: " + ex.getMessage());
            model.addAttribute("customers", java.util.List.of());
            return "customers/list";
        }
        return "customers/edit";
    }

    // Edit action (handles 409 for duplicate email)
    @PostMapping("/customers/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("customer") CustomerDto form, Model model) {
        try {
            var updated = api.updateCustomer(id, form);
            return "redirect:/customers/" + updated.getId();
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 409) {
                model.addAttribute("emailError", "Email already exists.");
                return "customers/edit";
            }
            model.addAttribute("error", "Failed to update: " + ex.getStatusCode().value() +
                    (ex.getResponseBodyAsString() != null ? " : " + ex.getResponseBodyAsString() : ""));
            return "customers/edit";
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to update: " + ex.getMessage());
            return "customers/edit";
        }
    }

    // ---------- DELETE ----------

    // Confirm delete page
    @GetMapping("/customers/{id}/delete")
    public String confirmDelete(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("customer", api.getCustomer(id));
            return "customers/delete";
        } catch (Exception ex) {
            model.addAttribute("error", "Failed to load customer: " + ex.getMessage());
            return "customers/view";
        }
    }

    // Delete action (shows helpful message if backend rejects)
    @PostMapping("/customers/{id}/delete")
    public String doDelete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            api.deleteCustomer(id);
            ra.addFlashAttribute("message", "Customer deleted.");
        } catch (org.springframework.web.client.HttpClientErrorException |
                 org.springframework.web.client.HttpServerErrorException ex) {
            String body = ex.getResponseBodyAsString();
            String msg = null;
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var root = mapper.readTree(body);
                var node = root.path("message");
                msg = node.isMissingNode() ? null : node.asText();
            } catch (Exception ignore) { }
            ra.addFlashAttribute("error",
                    (msg != null && !msg.isBlank())
                            ? msg
                            : "Delete failed (" + ex.getStatusCode().value() + "). Likely the customer has linked accounts or transactions.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Failed to delete: " + ex.getMessage());
        }
        return "redirect:/customers";
    }

    // Try to read {"message": "..."} from backend error JSON
    private String extractApiMessage(org.springframework.web.client.HttpClientErrorException ex) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(ex.getResponseBodyAsString());
            var node = root.path("message");
            return node.isMissingNode() ? null : node.asText();
        } catch (Exception ignore) { return null; }
    }
}
