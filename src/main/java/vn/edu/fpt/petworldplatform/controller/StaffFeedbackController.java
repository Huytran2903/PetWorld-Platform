package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffFeedbackController {

    @GetMapping("/feedback")
    public String showFeedbackManager() {
        return "staff/feedback/feedback-manager";
    }
}