package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;
import vn.edu.fpt.petworldplatform.service.*;
//import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
public class ProductController {

    @Autowired
    private PetService petService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;


    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private PetRepo petRepo;

    //Product List
    @GetMapping("/products")
    public String showAllProducts(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "categoryId", required = false) Integer categoryId) {

        // 1. Cấu hình phân trang: 10 sản phẩm mỗi trang, sắp xếp theo ID giảm dần (tùy chọn)
        int pageSize = 10;
        Sort sortOrder;
        // Đã sửa lỗi chính tả "acs" thành "asc"
        if ("asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").ascending();   // giá từ thấp -> cao
        } else if ("desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").descending();  // giá từ cao -> thấp
        } else {
            sortOrder = Sort.by("productId").descending(); // Mặc định mới nhất lên đầu
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
        Page<Product> productPage;

        // 2. Xử lý logic lấy dữ liệu (Phân trang cho Search và Filter)
        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasCategory = (categoryId != null && categoryId > 0);

        // 1. Trường hợp: Có CẢ Tên (Search) và Danh mục (Filter)
        if (hasKeyword && hasCategory) {
            productPage = productService.searchProductsByNameAndCategory(keyword.trim(), categoryId, pageable);
        }
        // 2. Trường hợp: CHỈ có Tên
        else if (hasKeyword) {
            productPage = productService.searchProductsByName(keyword.trim(), pageable);
        }
        // 3. Trường hợp: CHỈ có Danh mục
        else if (hasCategory) {
            productPage = productService.getProductsByCategory(categoryId, pageable);
        }
        // 4. Trường hợp: KHÔNG có gì (Lấy mặc định)
        else {
            productPage = productService.getAllProducts(pageable);
        }

        // Lấy toàn bộ danh mục để đẩy ra HTML tạo menu Sidebar/Dropdown
        model.addAttribute("categories", categoryService.getAllCategories());

        model.addAttribute("product", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("kw", keyword);
        model.addAttribute("sort", sort);
        // THÊM: Trả về category đang được chọn để HTML giữ trạng thái "active"
        model.addAttribute("selectedCategoryId", categoryId);

        return "/product/productList";
    }

    //Pet List
    @GetMapping("/pets")
    public String getAllPet(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "type", defaultValue = "", required = false) String type) { // Nhận số trang, mặc định là trang đầu (0)

        // 1. Cấu hình phân trang: 6 bản ghi mỗi trang, sắp xếp theo ID giảm dần
        int pageSize = 10;

        Sort sortOrder;
        if ("asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").ascending();   //giá từ thấp -> cao
        } else if ("desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").descending();
        } else {
            sortOrder = Sort.by("petID").descending();
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

        Page<Pets> petPage;

        // 2. Xử lý logic lấy dữ liệu (Phân trang cho cả Search và Get All)

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasType = (type != null && !type.trim().isEmpty());


        // 1. Trường hợp: Có CẢ Tên và Loại (Kết hợp filter)
        if (hasKeyword && hasType) {
            petPage = petService.findPetByNameAndType(keyword.trim(), type.trim(), pageable);
        }
        // 2. Trường hợp: CHỈ có Tên
        else if (hasKeyword) {
            petPage = petService.searchPetByName(keyword.trim(), pageable);
        }
        // 3. Trường hợp: CHỈ có Loại
        else if (hasType) {
            petPage = petService.getAvailablePetsByType(type.trim(), pageable);
        }
        // 4. Trường hợp: KHÔNG có gì (Lấy mặc định)
        else {
            petPage = petService.getAllPetWithPagination(pageable);
        }

        // 3. Đẩy dữ liệu ra Model
        model.addAttribute("pet", petPage.getContent());               // Danh sách thú cưng của trang hiện tại
        model.addAttribute("totalPages", petPage.getTotalPages());    // Tổng số trang (để vẽ nút chuyển trang)
        model.addAttribute("currentPage", page);                      // Số trang hiện tại
        model.addAttribute("kw", keyword);                            // Giữ từ khóa để khi chuyển trang không bị mất search
        model.addAttribute("totalElements", petPage.getTotalElements());
        model.addAttribute("sort", sort);
        model.addAttribute("selectedType", type);

        return "/product/petList";
    }


    //Product Detail
    @GetMapping("/product/detail/{id}")
    public String productDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("proDetail", productService.getProductById(id));
        return "product/product-detail";
    }

    //Pet Detail
    @GetMapping("/pet/detail/{id}")
    public String petDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("petDetail", petService.getPetById(id));
        return "product/pet-detail";
    }

}
