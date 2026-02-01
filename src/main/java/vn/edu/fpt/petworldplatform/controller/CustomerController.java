package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.service.CustomerService;

@Controller
public class CustomerController {

    @Autowired
    CustomerService customerService;

    @GetMapping("/profile")
    public String profileShow(HttpSession session, Model model) {
        Customer currentUser = (Customer) session.getAttribute("loggedInAccount");

        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);

        return "auth/viewProfile";
    }

    @GetMapping("/profile/edit")
    public String profileSetting(HttpSession session, Model model) {
        Customer currentUser = (Customer) session.getAttribute("loggedInAccount");

        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        return "auth/editProfile";
    }

    @PostMapping("/profile/do-edit")
    public String updateProfile(@ModelAttribute("user") Customer customerForm,
                                HttpSession session,
                                Model model) {

        Customer currentUser = (Customer) session.getAttribute("loggedInAccount");

        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            currentUser.setFullName(customerForm.getFullName());
            currentUser.setEmail(customerForm.getEmail());
            currentUser.setPhone(customerForm.getPhone());
            customerService.updateCustomer(currentUser);
            session.setAttribute("loggedInAccount", currentUser);


            return "redirect:/profile";

        } catch (Exception e) {

            e.printStackTrace();

            String message = e.getMessage();

            if (message != null && message.contains("ConstraintViolationException")) {
                model.addAttribute("error", "Invalid format! Phone number must be 10-12 digits.");
            } else if (message != null && (message.contains("Duplicate") || message.contains("UNIQUE"))) {
                model.addAttribute("error", "This email or phone number is already taken.");
            } else {
                model.addAttribute("error", "An unexpected error occurred. Please try again later.");
            }
            model.addAttribute("user", customerForm);

            return "auth/editProfile";
        }
    }

    //Order History
    @GetMapping("/customer/order-history")
    public String orderHistory() {
        return "customer/order-history";
    }

    //Cart
    @GetMapping("/customer/shopping-cart")
    public String shoppingCart() {
        return "customer/shopping-cart";
    }

    //Checkout Order
    @GetMapping("/customer/checkout-order")
    public String checkoutOrder() {
        return "customer/checkout-order";
    }
}

