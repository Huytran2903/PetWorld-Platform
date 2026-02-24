package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.service.*;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PetService petService;

    @Autowired
    private ProductService productService;



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
    public String getAllPets(Model model) {
        model.addAttribute("pets", petService.findAllPets());
        return "admin/managePet";
    }

    //Edit Pet
    @GetMapping("admin/pet/edit/{id}")
    public String updatePet(Model model, @PathVariable("id") Integer id) {
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

    //Delete Pet
    @GetMapping("/admin/pet/delete/{id}")
    public String deletePet(@PathVariable("id") Integer id) {
        petService.removePet(id);
        return "redirect:/admin/manage-pet";
    }


    @PostMapping("/admin/pet/save")
    public String savePet(@Validated @ModelAttribute("selectedPet") Pets pet,
                          BindingResult result, // Đưa lên ngay sau 'pet'
                          @RequestParam("imageFile") MultipartFile imageFile,
                          RedirectAttributes redirectAttributes,
                          @RequestParam("mode") String formMode,
                          Model model) {

        if(result.hasErrors()) {
            model.addAttribute("formMode", formMode);
            return "admin/pet-form";
        }

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
                    // Cách 1 (Nhanh): Nếu bên HTML có <input type="hidden" th:field="*{imageUrl}">
                    // thì pet.getImageUrl() đã có dữ liệu, không cần làm gì cả.

                    // Cách 2 (An toàn nhất): Lấy từ Database ra để chắc chắn không bị mất ảnh
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


    //Manage Product - OanhTP
    @GetMapping("/admin/manage-product")
    public String getAllProducts(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        return "admin/manageProduct";
    }

    //Create
    @GetMapping("/admin/product/new")
    public String createProduct(Model model) {
        model.addAttribute("selectedPro", new Product());

        model.addAttribute("cates", categoryService.getAllCategories());

        model.addAttribute("formMode", "add");
        return "admin/product-form";
    }

    @PostMapping("/admin/product/save")
    public String saveProduct(@Validated @ModelAttribute("selectedPro") Product product,
                              BindingResult result,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes redirectAttributes,
                              @RequestParam(value = "mode", required = false) String formMode,
                              Model model) {

        // NẾU CÓ LỖI VALIDATION (Ví dụ: để trống tên sản phẩm)
        if(result.hasErrors()) {
            model.addAttribute("formMode", formMode);

            model.addAttribute("cates", categoryService.getAllCategories());

            return "admin/product-form"; // Thay bằng tên file HTML form Product của bạn
        }

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
                product.setImageUrl("/uploads/" + fileName);

            } else {
                // 2. Logic Edit: Người dùng KHÔNG chọn ảnh mới -> Giữ nguyên ảnh cũ
                if (product.getProductId() != null) {
                    // Lấy sản phẩm cũ từ Database ra để lấy lại đường dẫn ảnh cũ
                    // Tuỳ vào cách bạn đặt tên hàm trong Service mà thay đổi cho phù hợp nhé:
                    Product oldProduct = productService.getProductById(product.getProductId());

                    if (oldProduct != null && (product.getImageUrl() == null || product.getImageUrl().isEmpty())) {
                        product.setImageUrl(oldProduct.getImageUrl());
                    }
                }
            }

            // Lưu sản phẩm vào Database
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("message", "Product saved successfully!");

        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu ảnh: " + e.getMessage());
        }


        return "redirect:/admin/manage-product";
    }

    @GetMapping("/admin/product/edit/{id}")
    public String updateProduct(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("selectedPro", productService.getProductById(id));

        model.addAttribute("cates", categoryService.getAllCategories());

        model.addAttribute("formMode", "edit");

        return "admin/product-form";
    }

    @GetMapping("/admin/product/delete/{id}")
    public String deleteProduct(@PathVariable("id") Integer id) {
        productService.deleteById(id);
        return "redirect:/admin/manage-product";
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
        boolean deleted = serviceTypeService.softDelete(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("message", "Service type has been deactivated.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot delete this Service Type because it has associated services or bookings.");
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
        boolean deleted = serviceItemService.softDelete(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("message", "Service has been deactivated.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Cannot delete this service because it has associated appointments.");
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
