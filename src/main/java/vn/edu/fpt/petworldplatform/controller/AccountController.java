package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.StaffService;

import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private StaffService staffService;


    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("customer", new Customer());
        return "auth/register";
    }


    @PostMapping("/do-register")
    public String handleRegister(@Valid @ModelAttribute Customer customer,
                                 BindingResult bindingResult,
                                 Model model) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldError().getDefaultMessage();
            model.addAttribute("error", errorMessage);
            return "auth/register";
        }

        if (customerService.checkEmailExists(customer.getEmail())) {
            model.addAttribute("error", "Email has already exist! Please try again!");
            return "auth/register";
        }

        try {
            customerService.registerNewCustomer(customer);

            return "redirect:/login?success_verify_sent";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @PostMapping("/do-login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              Model model, HttpSession session) {

        Optional<Customer> customerOpt = customerService.login(username, password);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            if (!customer.getIsActive()) {
                model.addAttribute("error", "Your account has been locked!");
                return "auth/login";
            }

            session.setAttribute("loggedInAccount", customer);
            return "redirect:/";
        }

        Optional<Staff> staffOpt = staffService.login(username, password);

        if (staffOpt.isPresent()) {
            Staff staff = staffOpt.get();

            if (staff.getPasswordHash().equals(password)) {

                if (!staff.getIsActive()) {
                    model.addAttribute("error", "Staff account is locked!");
                    return "auth/login";
                }

                session.setAttribute("loggedInStaff", staff);
                session.setAttribute("role", "admin");

                return "redirect:/admin/dashboard";
            }
        }

        model.addAttribute("error", "Invalid username or password");
        return "auth/login";
    }

    @GetMapping("/changePassword")
    public String changePassword() {
        return "auth/changePassForm"; // changePassForm.html
    }
}
