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

    @GetMapping("/create")
    public String showCreatePet(Model model) {
        // Gửi 1 phiếu trắng sang giao diện để người dùng điền
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    @PostMapping("/create")
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO) {
        // Gọi Service lưu vào Database
        petService.createPet(petDTO);
        return "redirect:/staff/pet/list";
    }

    @GetMapping("/list")
    public String showPetList(Model model) {
        model.addAttribute("pets", petService.getAllPets());
        return "staff/pet/pet-list";
    }
}