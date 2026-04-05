package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Pets; // Import Pets
import vn.edu.fpt.petworldplatform.service.PetService;
import java.io.IOException;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/pet")
public class StaffPetController {

    @Autowired
    private PetService petService;

    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @GetMapping("/create")
    public String showCreatePet(Model model) {
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @PostMapping("/create") // Hoặc đường dẫn hiện tại của bạn
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO, RedirectAttributes redirectAttributes) {
        try {
            petService.createPet(petDTO);

            redirectAttributes.addFlashAttribute("message", "Tạo thú cưng thành công!");
            return "redirect:/staff/pet/create";

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi upload ảnh: " + e.getMessage());
            return "redirect:/staff/pet/create"; // Quay lại trang tạo nếu lỗi

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/staff/pet/create";
        }
    }

    // Đổi Integer -> Long
    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @GetMapping("/detail")
    public String showPetDetail(@RequestParam("id") Integer id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pet", petService.getPetById(id));
            return "staff/pet/pet-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/pet/create";
        }
    }

    // Đổi Integer -> Long
    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @GetMapping("/update")
    public String showUpdatePet(@RequestParam("id") Integer id, Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pet", petService.getPetById(id));
            return "staff/pet/pet-update";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/pet/create";
        }
    }

    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @PostMapping("/update")
    public String handleUpdatePet(@ModelAttribute Pets pet) {
        petService.updatePet(pet);
        return "redirect:/staff/pet/detail?id=" + pet.getId();
    }

    // Đổi Integer -> Long
    @PreAuthorize("hasAuthority('MANAGE_PET')")
    @GetMapping("/history")
    public String showPetHistory(@RequestParam("id") Integer id, Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("pet", petService.getPetById(id));
            return "staff/pet/pet-history";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/pet/create";
        }
    }
}