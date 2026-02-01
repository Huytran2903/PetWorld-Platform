package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*; // Import này chứa @RequestParam, @GetMapping...
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Pet;
import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
@RequestMapping("/staff/pet")
public class StaffPetController {

    @Autowired
    private PetService petService;

    // --- 1. Xem danh sách thú cưng ---
    // URL: /staff/pet/list
    @GetMapping("/list")
    public String showPetList(Model model) {
        model.addAttribute("pets", petService.getAllPets());
        return "staff/pet/pet-list";
    }

    // --- 2. Tạo mới thú cưng (CREATE) ---

    // Hiển thị form tạo mới
    // URL: /staff/pet/create
    @GetMapping("/create")
    public String showCreatePet(Model model) {
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    // Xử lý khi bấm nút "Create Profile"
    @PostMapping("/create")
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO) {
        petService.createPet(petDTO);
        return "redirect:/staff/pet/list";
    }

    // --- 3. Xem chi tiết (DETAIL) - QUAN TRỌNG ĐỂ SỬA LỖI 404 ---
    // URL: /staff/pet/detail?id=1
    @GetMapping("/detail")
    public String showPetDetail(@RequestParam("id") Integer id, Model model) {
        Pet pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-detail";
    }

    // --- 4. Cập nhật thông tin (UPDATE) ---

    // Hiển thị form cập nhật (Load thông tin cũ lên)
    // URL: /staff/pet/update?id=1
    @GetMapping("/update")
    public String showUpdatePet(@RequestParam("id") Integer id, Model model) {
        Pet pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-update";
    }

    // Xử lý khi bấm nút "Save Changes"
    @PostMapping("/update")
    public String handleUpdatePet(@ModelAttribute Pet pet) {
        // Gọi Service để lưu thay đổi
        petService.updatePet(pet);

        // Lưu xong thì quay lại trang chi tiết của đúng con Pet đó
        return "redirect:/staff/pet/detail?id=" + pet.getId();
    }

    // --- 5. Xem lịch sử chăm sóc (HISTORY) ---
    // URL: /staff/pet/history?id=1
    @GetMapping("/history")
    public String showPetHistory(@RequestParam("id") Integer id, Model model) {
        Pet pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-history";
    }
}