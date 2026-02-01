package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
@RequestMapping("/staff/pet")
public class StaffPetController {

    @Autowired
    private PetService petService;

    // 1. Hiển thị danh sách
    @GetMapping("/list")
    public String showPetList(Model model) {
        // Đẩy list pet từ DB ra view
        model.addAttribute("pets", petService.getAllPets());
        return "staff/pet/pet-list";
    }

    // 2. Hiển thị form tạo mới
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        // Tạo object rỗng để hứng dữ liệu form
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    // 3. Xử lý khi bấm nút "Create Profile"
    @PostMapping("/create")
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO) {
        petService.createPet(petDTO);
        return "redirect:/staff/pet/list"; // Xong thì quay về danh sách
    }
}