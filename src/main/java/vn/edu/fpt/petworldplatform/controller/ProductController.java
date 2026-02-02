package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductController {

    //Product List
    @GetMapping("/products")
    public String showProductList(Model model) {
        model.addAttribute("formMode", "product");
        return "product/product-pet-list";
    }

    //Pet List
    @GetMapping("/pets")
    public String showPetList(Model model) {
        model.addAttribute("formMode", "pet");
        return "product/product-pet-list";
    }

    //Product Detail
    @GetMapping("/product/detail")
    public String productDetail() {
        return "product/product-detail";
    }

    //Pet Detail
    @GetMapping("/pet/detail")
    public String petDetail() {
        return "product/pet-detail";
    }



}
