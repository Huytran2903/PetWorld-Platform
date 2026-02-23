package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Pets; // Import Pets
import vn.edu.fpt.petworldplatform.service.PetService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/staff/pet")
public class StaffPetController {

    @Autowired
    private PetService petService;

    @GetMapping ("/list")
    public String showPetList(Model model) {
        List<Pets> allPets = petService.getAllPets();

        List<Pets> shopPets = new ArrayList<>();
        List<Pets> customerPets = new ArrayList<>();

        for (Pets p : allPets) {
            if (p.getOwner() == null) {
                shopPets.add(p);
            } else {
                customerPets.add(p);
            }
        }

        model.addAttribute("shopPets", shopPets);
        model.addAttribute("customerPets", customerPets);

        return "staff/pet/pet-list";
    }

    @GetMapping("/create")
    public String showCreatePet(Model model) {
        model.addAttribute("petDTO", new PetCreateDTO());
        return "staff/pet/pet-create";
    }

    @PostMapping("/create") // Hoặc đường dẫn hiện tại của bạn
    public String handleCreatePet(@ModelAttribute PetCreateDTO petDTO, RedirectAttributes redirectAttributes) {
        try {
            petService.createPet(petDTO);

            redirectAttributes.addFlashAttribute("message", "Tạo thú cưng thành công!");
            return "redirect:/staff/pet/list"; // Chuyển hướng về trang danh sách sau khi thành công

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