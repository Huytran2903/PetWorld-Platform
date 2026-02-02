package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.dto.ProfileFormDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.service.*;

@Controller
public class CustomerController {

    @Autowired
    CustomerService customerService;

    @Autowired
    PetService petService;

    @GetMapping("/profile")
    public String profileShow(@AuthenticationPrincipal Customer authUser, Model model) {
        if (authUser == null) {
            return "redirect:/login";
        }

        Customer currentFreshUser = customerService.findById(authUser.getCustomerId()).orElse(null);

        model.addAttribute("user", currentFreshUser);
        return "auth/viewProfile";
    }

    @GetMapping("/profile/edit")
    public String profileSetting(@AuthenticationPrincipal Customer authUser, Model model) {

        if (authUser == null) return "redirect:/login";

        Customer currentFreshUser = customerService.findById(authUser.getCustomerId()).orElse(null);
        if (currentFreshUser == null) return "redirect:/login?logout";

        ProfileFormDTO form = new ProfileFormDTO();
        form.setFullName(currentFreshUser.getFullName());
        form.setUsername(currentFreshUser.getUsername());
        form.setEmail(currentFreshUser.getEmail());
        form.setPhoneNumber(currentFreshUser.getPhone());

        model.addAttribute("user", form);

        return "auth/editProfile";
    }

    @PostMapping("/profile/do-edit")
    public String updateProfile(@Valid @ModelAttribute("user") ProfileFormDTO profileForm,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal Customer authUser,
                                HttpSession session,
                                Model model) {

        if (authUser == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {

            return "auth/editProfile";
        }

        try {

            Customer currentUser = customerService.findById(authUser.getCustomerId()).orElse(null);

            if (currentUser == null) {
                return "redirect:/login?logout"; //khó xảy ra nhưng cho chắc(User bị xóa khi edit)
            }


            currentUser.setFullName(profileForm.getFullName());
            currentUser.setEmail(profileForm.getEmail());
            currentUser.setPhone(profileForm.getPhoneNumber());

            customerService.updateCustomer(currentUser);

            session.setAttribute("loggedInAccount", currentUser);

            return "redirect:/profile?success";

        } catch (Exception e) {
            e.printStackTrace();

            String message = e.getMessage();
            if (message != null && (message.contains("Duplicate") || message.contains("UNIQUE"))) {
                model.addAttribute("error", "Email or phone number has already exist!");
            } else {
                model.addAttribute("error", "System error: " + e.getMessage());
            }

            model.addAttribute("user", profileForm);
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

    /**
     * Create Pet Profile (for customer - use case: no pets → redirect here).
     */
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

