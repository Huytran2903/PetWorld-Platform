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
import vn.edu.fpt.petworldplatform.repository.PetRepository;
import vn.edu.fpt.petworldplatform.repository.ProductRepository;
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
    private ProductRepository productRepository;

    @Autowired
    private PetRepository petRepository;

    //Product List
    @GetMapping("/products")
    public String showAllProducts(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "categoryId", required = false) Integer categoryId) {

        int pageSize = 10;
        Sort sortOrder;
        if ("asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").ascending();
        } else if ("desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").descending();
        } else {
            sortOrder = Sort.by("productId").descending();
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
        Page<Product> productPage;

        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasCategory = (categoryId != null && categoryId > 0);

        if (hasKeyword && hasCategory) {
            productPage = productService.searchProductsByNameAndCategory(keyword.trim(), categoryId, pageable);
        }
        else if (hasKeyword) {
            productPage = productService.searchProductsByName(keyword.trim(), pageable);
        }
        else if (hasCategory) {
            productPage = productService.getProductsByCategory(categoryId, pageable);
        }
        else {
            productPage = productService.getAllProducts(pageable);
        }

        model.addAttribute("categories", categoryService.getAllCategories());

        model.addAttribute("product", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("currentPage", page);
        model.addAttribute("kw", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("selectedCategoryId", categoryId);

        return "/product/productList";
    }

    //Pet List
    @GetMapping("/pets")
    public String showAllPets(
            Model model,
            @RequestParam(name = "kw", required = false, defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "type", defaultValue = "", required = false) String type) {

        int pageSize = 10;

        Sort sortOrder;
        if ("asc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").ascending();
        } else if ("desc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by("price").descending();
        } else {
            sortOrder = Sort.by("petID").descending();
        }

        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);

        Page<Pets> petPage;


        boolean hasKeyword = (keyword != null && !keyword.trim().isEmpty());
        boolean hasType = (type != null && !type.trim().isEmpty());


        if (hasKeyword && hasType) {
            petPage = petService.findPetByNameAndType(keyword.trim(), type.trim(), pageable);
        }
        else if (hasKeyword) {
            petPage = petService.searchPetByName(keyword.trim(), pageable);
        }
        else if (hasType) {
            petPage = petService.getAvailablePetsByType(type.trim(), pageable);
        }
        else {
            petPage = petService.getAllPetWithPagination(pageable);
        }

        model.addAttribute("pet", petPage.getContent());
        model.addAttribute("totalPages", petPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("kw", keyword);
        model.addAttribute("totalElements", petPage.getTotalElements());
        model.addAttribute("sort", sort);
        model.addAttribute("selectedType", type);

        return "/product/petList";
    }


    //Product Detail
    @GetMapping("/product/detail/{id}")
    public String getProductDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("proDetail", productService.findProductById(id));
        return "product/product-detail";
    }

    //Pet Detail
    @GetMapping("/pet/detail/{id}")
    public String getPetDetail(Model model, @PathVariable("id") Integer id) {
        model.addAttribute("petDetail", petService.getPetById(id));
        return "product/pet-detail";
    }

}
