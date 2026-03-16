package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import vn.edu.fpt.petworldplatform.dto.PetFormDTO;
import vn.edu.fpt.petworldplatform.dto.PetStatisticsDTO;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.dto.StaffFormDTO;
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
import java.time.LocalDate;
import java.util.ArrayList;
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

    private final StaffService staffService;
    private final RoleService roleService;
    @Autowired
    private ProductService productService;


    private final CustomerService customerService;
    private final CartService cartService;

    private final ServiceTypeService serviceTypeService;
    private final ServiceItemService serviceItemService;
    @Autowired
    private OrderService orderService;

    @GetMapping("/admin/dashboard")
    public String viewDashboard() {
        return "admin/admin-dashboard";
    }


    //Manage Pet - OanhTP
    //List
    @GetMapping("/admin/manage-pet")
    public String getAllPets(Model model, @RequestParam(value = "kw", required = false, defaultValue = "")                              String keyword) {
        if(!keyword.equals("")) {
            model.addAttribute("pets", petService.searchPetByName(keyword));
        }
        else {
            model.addAttribute("pets", petService.findAllPets());
        }
        return "admin/managePet";
    }

    //Edit Pet
    @GetMapping("admin/pet/edit/{id}")
    public String updatePet(Model model, @PathVariable("id") Integer id) {
        Pets petFromDb = petService.getPetById(id);

        if (petFromDb.getVaccinations() != null && !petFromDb.getVaccinations().isEmpty()) {
            petFromDb.setIsVaccinated(true);

            PetVaccinations firstVaccination = petFromDb.getVaccinations().get(0);

            if (firstVaccination != null && firstVaccination.getPerformedByStaff() != null) {
                petFromDb.setVaccinationStaffID(firstVaccination.getPerformedByStaff().getStaffId());
            } else {
                petFromDb.setVaccinationStaffID(null);
            }
        } else {
            petFromDb.setIsVaccinated(false);
        }

        model.addAttribute("selectedPet", petFromDb);
        model.addAttribute("formMode", "edit");
        model.addAttribute("staffList", staffService.getAllStaffs());

        return "admin/pet-form";
    }

    //Create Pet
    @GetMapping("/admin/pet/new")
    public String createPet(Model model) {
        model.addAttribute("selectedPet", new PetFormDTO());
        model.addAttribute("formMode", "add");
        model.addAttribute("staffList", staffService.getAllStaffs());
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
                          BindingResult result,
                          @RequestParam("imageFile") MultipartFile imageFile,
                          RedirectAttributes redirectAttributes,
                          @RequestParam("mode") String formMode,
                          Model model) {
        if (result.hasErrors()) {
            model.addAttribute("formMode", formMode);
            return "admin/pet-form";
        }

        try {
            if (pet.getPetID() != null) {
                Pets oldPet = petService.getPetById(pet.getPetID());
                if (oldPet != null) {
                    // Ráp lại danh sách tiêm phòng cũ để Hibernate không báo lỗi Orphan
                    pet.setVaccinations(oldPet.getVaccinations());

                    // Nếu KHÔNG chọn ảnh mới, lấy luôn đường dẫn ảnh cũ đắp vào
                    if (imageFile == null || imageFile.isEmpty()) {
                        pet.setImageUrl(oldPet.getImageUrl());
                    }
                }
            } else {
                // Đảm bảo list không bị null khi Thêm mới
                if (pet.getVaccinations() == null) {
                    pet.setVaccinations(new ArrayList<>());
                }
            }


            if (Boolean.TRUE.equals(pet.getIsVaccinated())) {
                if (pet.getVaccinations().isEmpty()) {
                    PetVaccinations newVaccine = new PetVaccinations();
                    newVaccine.setPet(pet);

                    // ---- BƠM DỮ LIỆU TỪ MODAL VÀO ENTITY ----
                    newVaccine.setVaccineName(pet.getVaccineName());
                    newVaccine.setNote(pet.getVaccineNote());

                    // Nếu có nhập ngày hẹn tiếp theo
                    if (pet.getNextDueDate() != null) {
                        newVaccine.setNextDueDate(pet.getNextDueDate()); // Ép kiểu LocalDate sang LocalDateTime nếu cần
                    }

                    // Mặc định ngày tiêm là ngay lúc bấm Save
                    newVaccine.setAdministeredDate(LocalDate.now());

                    // Xử lý Staff ID (Giả sử bạn có class Staff)
                    if (pet.getVaccinationStaffID() != null) {
                        // Bạn cần tạo 1 object Staff rỗng chỉ chứa ID để Hibernate map khóa ngoại
                        Staff performedBy = new Staff();
                        performedBy.setStaffId(pet.getVaccinationStaffID());
                        newVaccine.setPerformedByStaff(performedBy);
                    }
                    // ------------------------------------------

                    pet.getVaccinations().add(newVaccine);
                }
            } else {
                if (pet.getVaccinations() != null) {
                    pet.getVaccinations().clear();
                }
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                Path uploadPath = Paths.get("uploads");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                try (InputStream inputStream = imageFile.getInputStream()) {
                    Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                }

                // Lưu đường dẫn Web vào Database
                pet.setImageUrl("/uploads/" + fileName);
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
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra xem category có sản phẩm nào không (gọi qua service)
            boolean hasProducts = categoryService.hasProducts(id);

            if (hasProducts) {
                // Nếu có sản phẩm, không cho xóa và gửi thông báo lỗi
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa! Danh mục này vẫn đang chứa sản phẩm.");
            } else {
                // Nếu không có, tiến hành xóa
                categoryService.deleteCategoryById(id);
                redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi trong quá trình xóa.");
        }

        return "redirect:/admin/manage-categories";
    }


    //Manage Product - OanhTP
    @GetMapping("/admin/manage-product")
    public String getAllProducts(Model model, @RequestParam(value = "kw", required = false, defaultValue = "") String keyword) {

        if(!keyword.equals("")) {
            model.addAttribute("products", productService.searchProductsByName(keyword));
        }
        else {
            model.addAttribute("products", productService.getAllProducts());
        }

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


        if (result.hasErrors()) {
            model.addAttribute("formMode", formMode);

            model.addAttribute("cates", categoryService.getAllCategories());

            return "admin/product-form";
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
    public String showStaffList(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "5") int size, // 5 người/trang
            Model model) {

        Page<Staff> staffPage = staffService.getStaffsWithPaginationAndSearch(keyword, page, size);

        model.addAttribute("staffs", staffPage.getContent()); // Danh sách hiển thị trên bảng
        model.addAttribute("currentPage", page);              // Trang hiện tại
        model.addAttribute("totalPages", staffPage.getTotalPages()); // Tổng số trang
        model.addAttribute("totalItems", staffPage.getTotalElements()); // Tổng số nhân viên

        model.addAttribute("keyword", keyword);

        model.addAttribute("activeStaffList", staffService.getAvailableStaffs());

        return "admin/staff-manage";
    }

    @GetMapping("/admin/staff-manage/create")
    public String showStaffForm(Model model) {
        model.addAttribute("newStaff", new StaffFormDTO());
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("formMode", "create");
        return "admin/add-editStaffProfile";
    }

    @PostMapping("/admin/staff-manage/create")
    public String createStaff(@ModelAttribute("newStaff") StaffFormDTO staffDTO, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", roleService.getAllRoles());
            model.addAttribute("formMode", "create");

            return "admin/add-editStaffProfile";
        }

        try {
            staffService.createStaff(staffDTO);
            redirectAttributes.addFlashAttribute("message", "Staff account created and email sent successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating staff: " + e.getMessage());
        }

        return "redirect:/admin/staff-manage";
    }


    @GetMapping("/admin/edit-staff/{id}")
    public String showEditStaffForm(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("newStaff", staffService.getStaffDtoById(id));
        model.addAttribute("roles", roleService.getAllRoles());
        model.addAttribute("formMode", "edit");

        return "admin/add-editStaffProfile";
    }

    @PostMapping("/admin/staff-manage/update")
    public String updateStaff(@Valid @ModelAttribute("newStaff") StaffFormDTO staffDTO, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", roleService.getAllRoles());
            model.addAttribute("formMode", "edit");
            return "admin/add-editStaffProfile";
        }

        try {
            staffService.updateStaff(staffDTO);
            redirectAttributes.addFlashAttribute("message", "Staff updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating staff: " + e.getMessage());
        }

        return "redirect:/admin/staff-manage";
    }

    @PostMapping("/admin/staff/delete")
    public String deleteStaff(@RequestParam("staffId") Integer staffId,
                              @RequestParam(value = "transferStaffId", required = false) Integer transferStaffId,
                              RedirectAttributes ra) {
        staffService.deleteAndTransferWork(staffId, transferStaffId);

        ra.addFlashAttribute("success", "Xóa và bàn giao công việc thành công!");
        return "redirect:/admin/staff-manage";
    }

    @GetMapping("/admin/customer-manage")
    public String showCustomerList(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "5") int size,
            Model model) {

        Page<Customer> customerPage = customerService.getCustomersWithPaginationAndSearch(keyword, page, size);

        model.addAttribute("customers", customerPage.getContent());     // Danh sách hiển thị
        model.addAttribute("currentPage", page);                        // Trang hiện tại
        model.addAttribute("totalPages", customerPage.getTotalPages()); // Tổng số trang
        model.addAttribute("totalItems", customerPage.getTotalElements()); // Tổng số khách hàng

        model.addAttribute("keyword", keyword);

        return "admin/customer-manage";
    }

    // --- Edit Customer ---
    @GetMapping("/admin/customer/update-status/{id}")
    public String updateStatus(@PathVariable("id") int id, @RequestParam("isActive") boolean isActive, RedirectAttributes redirectAttributes) {
        try {

            customerService.updateCustomerStatus(id, isActive);
            String statusMsg = isActive ? "Unbanned" : "Banned";
            redirectAttributes.addFlashAttribute("message", "Customer has been " + statusMsg + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating status: " + e.getMessage());
        }

        return "redirect:/admin/customer-manage";
    }

    // --- Delete Customer ---
    @GetMapping("/admin/customer/delete/{id}")
    public String deleteCustomer(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            cartService.deleteCartByIdCustomer(id);
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("message", "Customer deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting customer: " + e.getMessage());
        }

        return "redirect:/admin/customer-manage";
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
    public String saveServiceType(@Valid @ModelAttribute("serviceType") ServiceType serviceType, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
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
    public String listServices(Model model, @RequestParam(required = false) String typeFilter, @RequestParam(required = false) Integer editId) {
        model.addAttribute("serviceTypes", serviceTypeService.findAll());
        List<ServiceItem> services = typeFilter != null && !typeFilter.isBlank() ? serviceItemService.findByServiceType(typeFilter) : serviceItemService.findAll();
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
    public String saveService(@Valid @ModelAttribute("service") ServiceItem service, BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
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
    public String showPetStatistics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            Model model) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ?
                LocalDate.parse(startDate) : LocalDate.of(2020, 1, 1);
        LocalDate end = (endDate != null && !endDate.isEmpty()) ?
                LocalDate.parse(endDate) : LocalDate.now();

        PetStatisticsDTO statistics = petService.getPetStatistics(start, end);

        model.addAttribute("statistics", statistics);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "admin/statistics/pet-report";
    }

    @GetMapping("/admin/statistics/pets/export")
    public ResponseEntity<String> exportPetStatistics(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        LocalDate start = (startDate != null && !startDate.isEmpty()) ?
                LocalDate.parse(startDate) : LocalDate.of(2020, 1, 1);
        LocalDate end = (endDate != null && !endDate.isEmpty()) ?
                LocalDate.parse(endDate) : LocalDate.now();

        PetStatisticsDTO statistics = petService.getPetStatistics(start, end);

        // Create CSV content
        StringBuilder csv = new StringBuilder();
        csv.append("Pet Statistics Report\n");
        csv.append("Date Range: ").append(start).append(" to ").append(end).append("\n\n");
        csv.append("Category,Dogs,Cats,Others,Total\n");

        // Service Pets
        long dogService = statistics.getDogStats() != null && !statistics.getDogStats().isEmpty() ?
                statistics.getDogStats().get(0).getCount() : 0;
        long catService = statistics.getCatStats() != null && !statistics.getCatStats().isEmpty() ?
                statistics.getCatStats().get(0).getCount() : 0;
        long otherService = statistics.getOtherStats() != null && !statistics.getOtherStats().isEmpty() ?
                statistics.getOtherStats().get(0).getCount() : 0;
        csv.append("Service Pets,").append(dogService).append(",").append(catService)
                .append(",").append(otherService).append(",").append(statistics.getTotalServicePets()).append("\n");

        // Sale Pets
        long dogSale = statistics.getDogStats() != null && statistics.getDogStats().size() > 1 ?
                statistics.getDogStats().get(1).getCount() : 0;
        long catSale = statistics.getCatStats() != null && statistics.getCatStats().size() > 1 ?
                statistics.getCatStats().get(1).getCount() : 0;
        long otherSale = statistics.getOtherStats() != null && statistics.getOtherStats().size() > 1 ?
                statistics.getOtherStats().get(1).getCount() : 0;
        csv.append("Sale Pets,").append(dogSale).append(",").append(catSale)
                .append(",").append(otherSale).append(",").append(statistics.getTotalSalePets()).append("\n");

        // Sold Pets
        long dogSold = statistics.getDogStats() != null && statistics.getDogStats().size() > 2 ?
                statistics.getDogStats().get(2).getCount() : 0;
        long catSold = statistics.getCatStats() != null && statistics.getCatStats().size() > 2 ?
                statistics.getCatStats().get(2).getCount() : 0;
        long otherSold = statistics.getOtherStats() != null && statistics.getOtherStats().size() > 2 ?
                statistics.getOtherStats().get(2).getCount() : 0;
        csv.append("Sold/Completed,").append(dogSold).append(",").append(catSold)
                .append(",").append(otherSold).append(",").append(statistics.getSoldPets()).append("\n");

        // Grand Total
        long dogTotal = statistics.getDogStats() != null ?
                statistics.getDogStats().stream().mapToLong(s -> s.getCount()).sum() : 0;
        long catTotal = statistics.getCatStats() != null ?
                statistics.getCatStats().stream().mapToLong(s -> s.getCount()).sum() : 0;
        long otherTotal = statistics.getOtherStats() != null ?
                statistics.getOtherStats().stream().mapToLong(s -> s.getCount()).sum() : 0;
        csv.append("Grand Total,").append(dogTotal).append(",").append(catTotal)
                .append(",").append(otherTotal).append(",").append(statistics.getTotalPets()).append("\n");

        // Set headers for CSV download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("pet-statistics-" + LocalDate.now() + ".csv")
                .build());

        return new ResponseEntity<>(csv.toString(), headers, HttpStatus.OK);
    }

    //Manage Order - OanhTP
    @GetMapping("/admin/manage-order")
    public String getAllOrder(Model model) {
        model.addAttribute("ord", orderService.getAllOrder());
        return "customer/manage-order";
    }

    @PostMapping("/admin/orders/update-status")
    public String updateStatus(@RequestParam("orderID") Integer orderID,
                               @RequestParam("status") String status,
                               RedirectAttributes redirectAttributes) {
        try {
            // 1. Gọi Service để thực hiện logic nghiệp vụ
            orderService.updateOrderStatusByAdmin(orderID, status);

            // 2. Nếu thành công, gửi thông báo xanh
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng #" + orderID + " thành công!");

        } catch (RuntimeException e) {
            // 3. Nếu Service ném lỗi (ví dụ: Đơn Paid không được về Pending)
            // Ta bắt lấy cái tin nhắn lỗi đó để hiển thị ra màn hình
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Phòng hờ các lỗi hệ thống khác
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
        }

        // 4. Redirect về trang danh sách (Để tránh việc F5 gây lặp lại request)
        return "redirect:/admin/manage-order";
    }

}
