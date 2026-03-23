package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
import vn.edu.fpt.petworldplatform.util.SecuritySupport;

import java.util.List;
import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SecuritySupport securitySupport;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();


    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("customer", new Customer());
        return "auth/register";
    }


    @PostMapping("/do-register") // Đảm bảo mapping đúng với form
    public String handleRegister(@Valid @ModelAttribute("customer") Customer customer,
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


            model.addAttribute("showOtpModal", true);

            model.addAttribute("emailRegister", customer.getEmail());

            model.addAttribute("message", "Registration successful! Please check your email for OTP.");

            return "auth/register";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/do-verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email, @RequestParam("otp") String otp, RedirectAttributes redirectAttributes) {

        boolean isVerified = customerService.verifyOtp(email, otp);

        if (isVerified) {
            redirectAttributes.addFlashAttribute("message", "Account verified successfully! Please log in.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorOtp", "Invalid or expired OTP code!");

            redirectAttributes.addFlashAttribute("showOtpModal", true);
            redirectAttributes.addFlashAttribute("emailRegister", email);

            return "redirect:/register";
        }
    }


    @PostMapping("/resend-otp")
    public String resendRegisterOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            customerService.resendOtp(email, "REGISTER");
            redirectAttributes.addFlashAttribute("message", "A new OTP has been sent to your email!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorOtp", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("showOtpModal", true);
        redirectAttributes.addFlashAttribute("emailRegister", email);

        return "redirect:/register";
    }


    @PostMapping("/resend-forgot-password-otp")
    public String resendForgotOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            customerService.resendOtp(email, "FORGOT_PASSWORD");
            redirectAttributes.addFlashAttribute("message", "A new password reset OTP has been sent!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorOtp", e.getMessage());
        }

        redirectAttributes.addFlashAttribute("shouldOpenOtp", true);
        redirectAttributes.addFlashAttribute("emailForgot", email);

        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }


    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model) {
        Customer authUser = securitySupport.getCurrentAuthenticatedCustomer();

        if (authUser == null) return "redirect:/login";

        model.addAttribute("user", authUser);
        model.addAttribute("formMode", "CHANGE");
        return "auth/password-form-shared";
    }

    @PostMapping("/profile/change-password")
    public String processChangePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model, RedirectAttributes redirectAttributes) {

        Customer authUser = securitySupport.getCurrentAuthenticatedCustomer();
        if (authUser == null) return "redirect:/login";

        Customer currentUser = customerService.findById(authUser.getCustomerId()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "New password and confirm password do not match!");
            model.addAttribute("user", currentUser);
            model.addAttribute("formMode", "CHANGE");
            return "auth/password-form-shared";
        }

        if (newPassword.equals(oldPassword)) {
            model.addAttribute("error", "New password cannot be the same as the current password!");
            model.addAttribute("user", currentUser);
            model.addAttribute("formMode", "CHANGE");
            return "auth/password-form-shared";
        }

        if (!customerService.verifyOldPassword(currentUser, oldPassword)) {
            model.addAttribute("error", "Incorrect current password!");
            model.addAttribute("user", currentUser);
            model.addAttribute("formMode", "CHANGE");
            return "auth/password-form-shared";
        }

        customerService.updatePassword(currentUser, newPassword);

        redirectAttributes.addFlashAttribute("message", "Password changed successfully!");
        return "redirect:/profile";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
        try {
            customerService.sendResetPasswordEmail(email);


            redirectAttributes.addFlashAttribute("message", "OTP has been sent to " + email);
            redirectAttributes.addFlashAttribute("openOtpModal", true);

            redirectAttributes.addFlashAttribute("emailForgot", email);

            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/verify-forgot-password-otp")
    public String verifyOtpForPassword(@RequestParam("otp") String otp,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        String email = (String) session.getAttribute("resetEmail");
        Customer customer = customerService.getByResetPasswordToken(otp);

        if (customer == null || !customer.getEmail().equals(email)) {
            System.out.println("DEBUG: OTP sai hoặc email không khớp!");
            redirectAttributes.addFlashAttribute("error", "Invalid or expired OTP code!");
            redirectAttributes.addFlashAttribute("openOtpModal", true);
            return "redirect:/login";
        }

        System.out.println("DEBUG: OTP chuẩn! Đang chuyển hướng tới trang đổi mật khẩu...");
        return "redirect:/reset-password?token=" + otp;
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, HttpSession session, Model model) {
        Customer customer = customerService.getByResetPasswordToken(token);

        if (customer == null) {
            return "redirect:/login?error=invalid_token";
        }

        String email = (String) session.getAttribute("resetEmail");

        model.addAttribute("token", token);
        model.addAttribute("email", email);
        model.addAttribute("formMode", "RESET_OTP");

        return "auth/password-form-shared";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("otp") String otp,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        String email = (String) session.getAttribute("resetEmail");
        if (email == null) return "redirect:/login";

        Customer customer = customerService.getByResetPasswordToken(otp);

        if (customer == null || !customer.getEmail().equals(email)) {
            model.addAttribute("error", "Invalid or expired OTP code!");
            model.addAttribute("email", email);
            model.addAttribute("formMode", "RESET_OTP");
            return "auth/password-form-shared";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Confirm password does not match!");
            model.addAttribute("email", email);
            model.addAttribute("formMode", "RESET_OTP");
            return "auth/password-form-shared";
        }

        customerService.updatePassword(customer, newPassword);

        session.removeAttribute("resetEmail");

        redirectAttributes.addFlashAttribute("message", "Password reset successfully! Please login.");
        return "redirect:/login";
    }
}