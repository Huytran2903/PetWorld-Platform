package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

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
}
