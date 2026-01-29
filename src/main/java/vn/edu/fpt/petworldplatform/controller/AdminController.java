package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/staff/add")
    public String addNewStaff() {
        return "admin/add-editStaffProfile";
    }

    @GetMapping("/staff/edit")
    public String editStaffProfile() {
        return "admin/add-editStaffProfile";
    }
}
