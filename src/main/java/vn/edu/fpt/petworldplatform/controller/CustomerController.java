package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerController {

    @GetMapping("/profile")
    public String profileSetting() {
        return "auth/viewProfile"; // viewProfile.html
    }
}
