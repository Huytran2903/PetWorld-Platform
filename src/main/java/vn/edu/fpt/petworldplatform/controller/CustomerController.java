package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomerController {

    @GetMapping("/profile")
    public String profileShow() {
        return "auth/viewProfile"; // viewProfile.html
    }

    @GetMapping("/profile/edit")
    public String profileSetting() {
        return "auth/editProfile";
    }

    //Order History
    @GetMapping("/customer/order-history")
    public String orderHistory() {
        return "customer/order-history";
    }
}

