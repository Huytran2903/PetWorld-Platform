package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
public class CustomerController {

    @Autowired
    CustomerService customerService;

    @Autowired
    PetService petService;

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

    /** Create Pet Profile (for customer - use case: no pets → redirect here). */
    @GetMapping("/customer/pet/create")
    public String showCreatePet(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        PetCreateDTO dto = new PetCreateDTO();
        dto.setCreatePetOwnerType("customer");
        dto.setOwnerId(customer.getCustomerId());
        model.addAttribute("petDTO", dto);
        return "customer/pet-create";
    }

    @PostMapping("/customer/pet/create")
    public String handleCreatePet(HttpSession session, @ModelAttribute PetCreateDTO petDTO, RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        petDTO.setCreatePetOwnerType("customer");
        petDTO.setOwnerId(customer.getCustomerId());
        petService.createPet(petDTO);
        redirectAttributes.addFlashAttribute("message", "Pet profile created. You can now book a service appointment.");
        return "redirect:/appointment/booking";
    }
}

