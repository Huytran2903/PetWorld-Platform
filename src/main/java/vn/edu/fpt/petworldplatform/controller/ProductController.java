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
    private ProductRepo productRepo;

    @Autowired
    private PetRepo petRepo;

    //Product List
    @GetMapping("/products")
    public String getAllProducts(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page) { // Thêm tham số nhận số trang

        // 1. Cấu hình phân trang: 10 sản phẩm mỗi trang, sắp xếp theo ID giảm dần (tùy chọn)
        int pageSize = 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("productId").descending());

        Page<Product> productPage;

        // 2. Gọi service và truyền thêm đối tượng pageable vào
        if(!keyword.trim().isEmpty()) {
            productPage = productService.searchProductsByName(keyword, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }

        // 3. Đẩy dữ liệu ra Model để Thymeleaf xử lý
        model.addAttribute("product", productPage.getContent());            // Danh sách sản phẩm hiển thị trên trang này
        model.addAttribute("totalPages", productPage.getTotalPages());      // Tổng số trang
        model.addAttribute("totalElements", productPage.getTotalElements());// Tổng số sản phẩm (dùng để hiện "X products found")
        model.addAttribute("currentPage", page);                            // Số trang hiện tại
        model.addAttribute("kw", keyword);                                  // Giữ lại từ khóa tìm kiếm

        return "/product/productList";
    }

    //Pet List
    @GetMapping("/pets")
    public String getAllPet(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page) { // Nhận số trang, mặc định là trang đầu (0)

        // 1. Cấu hình phân trang: 6 bản ghi mỗi trang, sắp xếp theo ID giảm dần
        int pageSize = 10;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("petID").ascending());

        Page<Pets> petPage;

        // 2. Xử lý logic lấy dữ liệu (Phân trang cho cả Search và Get All)
        if (!keyword.trim().isEmpty()) {
            petPage = petService.searchPetByName(keyword, pageable);
        } else {
            petPage = petService.getAllPetWithPagination(pageable);
        }

        // 3. Đẩy dữ liệu ra Model
        model.addAttribute("pet", petPage.getContent());               // Danh sách thú cưng của trang hiện tại
        model.addAttribute("totalPages", petPage.getTotalPages());    // Tổng số trang (để vẽ nút chuyển trang)
        model.addAttribute("currentPage", page);                      // Số trang hiện tại
        model.addAttribute("kw", keyword);                            // Giữ từ khóa để khi chuyển trang không bị mất search
        model.addAttribute("totalElements", petPage.getTotalElements());

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
