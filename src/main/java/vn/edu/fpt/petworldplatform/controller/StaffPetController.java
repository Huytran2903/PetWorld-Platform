package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/pet") // Prefix chung cho tất cả các URL bên dưới
public class StaffPetController {

    // 1. Trang danh sách (Pet List)
    // URL: http://localhost:8080/staff/pet/list
    @GetMapping("/list")
    public String showPetList() {
        return "staff/pet/pet-list";
    }

    // 2. Trang tạo mới (Create Profile)
    // URL: http://localhost:8080/staff/pet/create
    @GetMapping("/create")
    public String showCreatePet() {
        return "staff/pet/pet-create";
    }

    // 3. Trang chi tiết (Pet Detail)
    // URL: http://localhost:8080/staff/pet/detail
    @GetMapping("/detail")
    public String showPetDetail() {
        return "staff/pet/pet-detail";
    }

    // 4. Trang cập nhật (Update Profile)
    // URL: http://localhost:8080/staff/pet/update
    @GetMapping("/update")
    public String showUpdatePet() {
        return "staff/pet/pet-update";
    }

    // 5. Trang lịch sử chăm sóc (Care History)
    // URL: http://localhost:8080/staff/pet/history
    @GetMapping("/history")
    public String showPetHistory() {
        return "staff/pet/pet-history";
    }
}