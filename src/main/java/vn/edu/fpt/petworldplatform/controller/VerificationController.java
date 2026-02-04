package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.service.CustomerService;

@Controller
@RequiredArgsConstructor
public class VerificationController {

    private final CustomerService customerService;

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token) {

        String result = customerService.verifyEmailToken(token);

        switch (result) {
            case "success":
                return "redirect:/login?verified=true"; // Thành công
            case "expired":
                return "redirect:/login?error=token_expired"; // Hết hạn
            case "invalid":
            default:
                return "redirect:/login?error=invalid_token"; // Token sai
        }
    }
}