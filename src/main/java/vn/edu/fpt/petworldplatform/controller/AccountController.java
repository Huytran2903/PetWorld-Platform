package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.service.CustomerService;
import vn.edu.fpt.petworldplatform.service.StaffService;

import java.util.List;
import java.util.Optional;

@Controller
public class AccountController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private StaffService staffService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();


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
                              Model model,
                              HttpSession session,
                              HttpServletRequest request,
                              HttpServletResponse response) {
        Optional<Customer> customerOpt = customerService.login(username, password);

        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            if (!customer.getIsActive()) {
                model.addAttribute("error", "Your account has been locked!");
                return "auth/login";
            }

            session.setAttribute("loggedInAccount", customer);


            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(customer, null, authorities);

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authToken);
            SecurityContextHolder.setContext(context);

            securityContextRepository.saveContext(context, request, response);

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
                session.setAttribute("role", staff.getRole());

                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_STAFF"));
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(staff, null, authorities);

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);

                securityContextRepository.saveContext(context, request, response);

                return "redirect:/admin/dashboard";
            }
        }

        model.addAttribute("error", "Invalid username or password");
        return "auth/login";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal Customer authUser, Model model) {
        if (authUser == null) return "redirect:/login";

        Customer currentUser = customerService.findById(authUser.getCustomerId()).orElse(null);

        model.addAttribute("user", currentUser);
        model.addAttribute("formMode", "CHANGE");
        return "auth/password-form-shared";
    }

    @PostMapping("/profile/change-password")
    public String processChangePassword(@AuthenticationPrincipal Customer authUser,
                                        @RequestParam("oldPassword") String oldPassword,
                                        @RequestParam("newPassword") String newPassword,
                                        @RequestParam("confirmPassword") String confirmPassword,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {

        if (authUser == null) return "redirect:/login";

        Customer currentUser = customerService.findById(authUser.getCustomerId()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp!");
            model.addAttribute("user", currentUser);
            model.addAttribute("formMode", "CHANGE");
            return "auth/password-form-shared";
        }

        if (!customerService.verifyOldPassword(currentUser, oldPassword)) {
            model.addAttribute("error", "old password incorrect!");
            model.addAttribute("user", currentUser);
            model.addAttribute("formMode", "CHANGE");
            return "auth/password-form-shared";
        }

        customerService.updatePassword(currentUser, newPassword);

        redirectAttributes.addFlashAttribute("message", "Đổi mật khẩu thành công!");
        return "redirect:/profile";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            customerService.sendResetPasswordEmail(email);
            redirectAttributes.addFlashAttribute("message", "Link for reset password has been sent: " + email);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/login";
    }


    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Customer customer = customerService.getByResetPasswordToken(token);

        if (customer == null) {
            model.addAttribute("error", "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn!");
            return "auth/login";
        }

        model.addAttribute("token", token);
        model.addAttribute("formMode", "RESET");
        return "auth/password-form-shared";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        Customer customer = customerService.getByResetPasswordToken(token);
        if (customer == null) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn!");
            return "auth/login";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("token", token);
            model.addAttribute("formMode", "RESET");
            return "auth/password-form-shared";
        }


        customerService.updatePassword(customer, newPassword);


        redirectAttributes.addFlashAttribute("message", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}