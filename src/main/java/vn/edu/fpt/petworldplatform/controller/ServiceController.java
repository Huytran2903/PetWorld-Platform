package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServiceController {

    @GetMapping("/register")
    public String register() {
        return "auth/register"; // register.html
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login"; // login.html
    }

    @GetMapping("/changePassword")
    public String changePassword() {
        return "auth/changePassForm"; // changePassForm.html
    }
}
