package vn.edu.fpt.petworldplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;
import vn.edu.fpt.petworldplatform.entity.Categories;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.entity.ServiceType;
import vn.edu.fpt.petworldplatform.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PetService petService;

    private final CustomerService customerService;
    private final ServiceTypeService serviceTypeService;
    private final ServiceItemService serviceItemService;

    @GetMapping("/admin/dashboard")
    public String viewDashboard() {
        return "admin/admin-dashboard";
    }

    @GetMapping("/admin/add-staff")
    public String addNewStaff() {
        return "admin/add-editStaffProfile";
    }

    @GetMapping("/admin/edit-staff")
    public String editStaffProfile() {
        return "admin/add-editStaffProfile";
    }

    //Manage Pet - OanhTP
    //List
    @GetMapping("/admin/manage-pet")
    public String getAllPet(Model model) {
        model.addAttribute("pets", petService.getAllPets2());
        return "admin/managePet";
    }

    //Edit Pet
    @GetMapping("admin/pet/edit/{id}")
    public String updatePet(Model model, @PathVariable("id") Long id) {
        model.addAttribute("selectedPet", petService.getPetById(id));
        model.addAttribute("formMode", "edit");
        return "admin/pet-form";
    }

    //Create Pet
    @GetMapping("/admin/pet/new")
    public String createPet(Model model) {
        model.addAttribute("selectedPet", new PetFormDTO());
        model.addAttribute("formMode", "add");
        return "admin/pet-form";
    }


    @PostMapping("/admin/pet/save")
    public String savePet(@ModelAttribute("selectedPet") Pets pet,
                          @RequestParam("imageFile") MultipartFile imageFile,
                          RedirectAttributes redirectAttributes) {
        try {
            // 1. Kiểm tra nếu người dùng CÓ chọn file ảnh mới
            if (imageFile != null && !imageFile.isEmpty()) {

                // Tạo đường dẫn tới thư mục "uploads" nằm ngay tại thư mục gốc dự án
                Path uploadPath = Paths.get("uploads");

                // Tạo thư mục nếu chưa tồn tại
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Tạo tên file ngẫu nhiên (UUID) để tránh trùng lặp
                String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                // Lưu file vật lý vào ổ cứng
                try (InputStream inputStream = imageFile.getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }

                // [QUAN TRỌNG] Lưu đường dẫn Web vào Database (Ví dụ: /uploads/abc.jpg)
                pet.setImageUrl("/uploads/" + fileName);

            } else {
                // 2. Logic Edit: Người dùng KHÔNG chọn ảnh mới -> Giữ nguyên ảnh cũ
                if (pet.getPetID() != null) {
                    Pets oldPet = petService.getPetById(pet.getPetID());
                    if (oldPet != null && (pet.getImageUrl() == null || pet.getImageUrl().isEmpty())) {
                        pet.setImageUrl(oldPet.getImageUrl());
                    }
                }
            }

            petService.savePet(pet);
            redirectAttributes.addFlashAttribute("message", "Pet saved successfully!");

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }

        return "redirect:/admin/manage-pet";
    }


    //Manage Product - OanhTP
    @GetMapping("/admin/manage-product")
    public String manageProduct() {
        return "admin/manageProduct";
    }

    //Manage Categories - OanhTP
    //List
    @GetMapping("/admin/manage-categories")
    public String getAllCategories(Model model) {

        model.addAttribute("categories", categoryService.getAllCategories());

        return "admin/manageCategories";
    }

    //Edit
    @GetMapping("/admin/category/edit/{id}")
    public String editCategory(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("selectedCate", categoryService.getCategoryById(id));
        model.addAttribute("formMode", "edit");

        return "admin/category-form";
    }


    //Create
    @GetMapping("/admin/category/new")
    public String createCategory(Model model) {
        model.addAttribute("selectedCate", new Categories());

        model.addAttribute("formMode", "add");
        return "admin/category-form";
    }

    //Save
    @PostMapping("/admin/category/save")
    public String saveCategory(@Validated @ModelAttribute("selectedCate") Categories cate, BindingResult result, Model model, @RequestParam("mode") String formMode) {

        if (result.hasErrors()) {
            model.addAttribute("formMode", formMode);
            return "admin/category-form";
        }

        categoryService.saveCategory(cate);
        return "redirect:/admin/manage-categories";
    }

    //Delete
    @GetMapping("/admin/category/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id) {
        categoryService.deleteCategoryById(id);
        return "redirect:/admin/manage-categories";
    }


    //Create Product - OanhTP
    @GetMapping("/admin/product/new")
    public String saveProduct() {
        return "admin/product-form";
    }


    @GetMapping("/admin/staff-manage")
    public String showStaffList() {
        return "admin/staff-manage";
    }

    @GetMapping("/admin/appointment-manage")
    public String showAppointmentList() {
        return "admin/appt-manage";
    }

    @GetMapping("/admin/appointment-manage/detail")
    public String showAppointmentDetail() {
        return " appointment/appt-detail";
    }

    @GetMapping("/admin/customer-manage")
    public String showCustomerList(Model model) {
        model.addAttribute("customers", customerService.getAllCustomer());
        return "admin/customer-manage";
    }


    // Service Manager
    @GetMapping("/admin/service-manager")
    public String serviceManager() {
        return "admin/service-manager";
    }

    // Manage Service Types (list + form)
    @GetMapping("/admin/service-type")
    public String listServiceTypes(Model model, @RequestParam(required = false) Integer editId) {
        model.addAttribute("serviceTypes", serviceTypeService.findAll());
        if (editId != null) {
            serviceTypeService.findById(editId).ifPresent(st -> {
                model.addAttribute("serviceType", st);
                model.addAttribute("openModal", true);
            });
        }
        if (!model.containsAttribute("serviceType")) {
            model.addAttribute("serviceType", new ServiceType());
        }
        return "admin/service-type";
    }

    @PostMapping("/admin/service-type/save")
    public String saveServiceType(
            @Valid @ModelAttribute("serviceType") ServiceType serviceType,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer id = serviceType.getId();
        if (id != null && id == 0) serviceType.setId(null);
        if (!bindingResult.hasFieldErrors("name")) {
            if (serviceTypeService.isNameDuplicate(serviceType.getName().trim(), serviceType.getId())) {
                bindingResult.rejectValue("name", "duplicate", "Name already exists");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("serviceTypes", serviceTypeService.findAll());
            model.addAttribute("serviceType", serviceType);
            model.addAttribute("openModal", true);
            return "admin/service-type";
        }
        serviceType.setName(serviceType.getName().trim());
        serviceTypeService.save(serviceType);
        redirectAttributes.addFlashAttribute("message", "Service type saved successfully.");
        return "redirect:/admin/service-type";
    }

    @PostMapping("/admin/service-type/delete/{id}")
    public String deleteServiceType(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        ServiceTypeService.DeleteResult result = serviceTypeService.deleteOrDeactivate(id);
        if (!result.isOk()) {
            redirectAttributes.addFlashAttribute("error", "Service type not found.");
        } else if (result.isDeleted()) {
            redirectAttributes.addFlashAttribute("message", "Service type has been permanently deleted.");
        } else {
            redirectAttributes.addFlashAttribute("message", "Service type is in use (linked to " + result.getUsedServices() + " services or " + result.getUsedAppointments() + " appointments). It has been deactivated instead of deleted.");
        }
        return "redirect:/admin/service-type";
    }

    // UC-26: Manage Services (service items: price, duration, etc.)
    @GetMapping("/admin/services")
    public String listServices(Model model,
                               @RequestParam(required = false) String typeFilter,
                               @RequestParam(required = false) Integer editId) {
        model.addAttribute("serviceTypes", serviceTypeService.findAll());
        List<ServiceItem> services = typeFilter != null && !typeFilter.isBlank()
                ? serviceItemService.findByServiceType(typeFilter)
                : serviceItemService.findAll();
        model.addAttribute("services", services);
        model.addAttribute("typeFilter", typeFilter != null ? typeFilter : "");
        if (editId != null) {
            serviceItemService.findById(editId).ifPresent(svc -> {
                model.addAttribute("service", svc);
                model.addAttribute("openModal", true);
            });
        }
        if (!model.containsAttribute("service")) {
            model.addAttribute("service", ServiceItem.builder().durationMinutes(30).isActive(true).build());
        }
        return "admin/service-list";
    }

    @PostMapping("/admin/service/save")
    public String saveService(
            @Valid @ModelAttribute("service") ServiceItem service,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        Integer id = service.getId();
        if (id != null && id == 0) service.setId(null);
        if (service.getServiceType() != null && !service.getServiceType().isBlank() && !bindingResult.hasFieldErrors("name")) {
            if (serviceItemService.isNameDuplicate(service.getName().trim(), service.getServiceType(), service.getId())) {
                bindingResult.rejectValue("name", "duplicate", "A service with this name already exists in the selected type.");
            }
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("serviceTypes", serviceTypeService.findAll());
            model.addAttribute("services", serviceItemService.findAll());
            model.addAttribute("typeFilter", "");
            model.addAttribute("openModal", true);
            return "admin/service-list";
        }
        service.setName(service.getName().trim());
        if (service.getServiceType() != null) service.setServiceType(service.getServiceType().trim());
        serviceItemService.save(service);
        redirectAttributes.addFlashAttribute("message", "Service saved successfully.");
        return "redirect:/admin/services";
    }

    @PostMapping("/admin/service/delete/{id}")
    public String deleteService(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        ServiceItemService.DeleteResult result = serviceItemService.deleteOrDeactivate(id);
        if (!result.isOk()) {
            redirectAttributes.addFlashAttribute("error", "Service not found.");
        } else if (result.isDeleted()) {
            redirectAttributes.addFlashAttribute("message", "Service has been permanently deleted.");
        } else {
            redirectAttributes.addFlashAttribute("message", "Service is in use (linked to " + result.getUsedAppointments() + " appointments). It has been deactivated instead of deleted.");
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/admin/statistics/pets")
    public String showPetStatistics(Model model) {
        long totalPets = petService.getTotalPets();

        List<Object[]> stats = petService.getPetStatsBySpecies();

        long dogCount = 0;
        long catCount = 0;
        long otherCount = 0;

        if (stats != null) {
            for (Object[] row : stats) {
                String species = (String) row[0];
                long count = (Long) row[1];

                if (species != null) {
                    if (species.equalsIgnoreCase("Dog")) {
                        dogCount += count;
                    } else if (species.equalsIgnoreCase("Cat")) {
                        catCount += count;
                    } else {
                        otherCount += count;
                    }
                } else {
                    otherCount += count;
                }
            }
        }

        model.addAttribute("totalPets", totalPets);
        model.addAttribute("dogCount", dogCount);
        model.addAttribute("catCount", catCount);
        model.addAttribute("otherCount", otherCount);

        model.addAttribute("statsBySpecies", stats);

        return "admin/statistics/pet-report";
    }

}
