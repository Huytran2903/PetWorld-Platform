package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Pets; // Import Pets
import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
@RequestMapping("/staff/pet")
public class StaffPetController {

    @Autowired
    private PetService petService;

    @GetMapping("/list")
    public String showPetList(Model model) {
        model.addAttribute("pets", petService.getAllPets());
        return "staff/pet/pet-list";
    }

    @GetMapping("/create")
    public String showCreatePet(Model model) {
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    @PostMapping("/create")
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO) {
        petService.createPet(petDTO);
        return "redirect:/staff/pet/list";
    }

    // Đổi Integer -> Long
    @GetMapping("/detail")
    public String showPetDetail(@RequestParam("id") Long id, Model model) {
        Pets pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-detail";
    }

    // Đổi Integer -> Long
    @GetMapping("/update")
    public String showUpdatePet(@RequestParam("id") Long id, Model model) {
        Pets pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-update";
    }

    @PostMapping("/update")
    public String handleUpdatePet(@ModelAttribute Pets pet) {
        petService.updatePet(pet);
        return "redirect:/staff/pet/detail?id=" + pet.getId();
    }

    // Đổi Integer -> Long
    @GetMapping("/history")
    public String showPetHistory(@RequestParam("id") Long id, Model model) {
        Pets pet = petService.getPetById(id);
        model.addAttribute("pet", pet);
        return "staff/pet/pet-history";
    }
}