package vn.edu.fpt.petworldplatform.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.ServiceType;
import vn.edu.fpt.petworldplatform.service.ServiceTypeService;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ServiceTypeService serviceTypeService;

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
    @GetMapping("/admin/manage-pet")
    public String managePet() {
        return "admin/managePet";
    }

    //Manage Product - OanhTP
    @GetMapping("/admin/manage-product")
    public String manageProduct() {
        return "admin/manageProduct";
    }

    //Manage Categories - OanhTP
   @GetMapping("/admin/manage-categories")
   public String manageCategories() {
        return "admin/manageCategories";
   }

    //Create Pet - OanhTP
    @GetMapping("/admin/pet/save")
    public String savePet() {
        return "admin/pet-form";
    }

    //Create Product - OanhTP
    @GetMapping("/admin/product/save")
    public String saveProduct() {
        return "admin/product-form";
    }

    //Create Category - OanhTP
    @GetMapping("/admin/category/save")
    public String saveCategory() {
        return "admin/category-form";
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
    public String showCustomerList() {
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
        boolean deleted = serviceTypeService.softDelete(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("message", "Service type has been deactivated.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot delete this Service Type because it has associated services or bookings.");
        }
        return "redirect:/admin/service-type";
    }
}
