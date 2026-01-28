package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/appointment")
public class BookingController {

    @GetMapping("/booking")
    public String bookingPage() {
        return "appointment/booking";
    }
}
