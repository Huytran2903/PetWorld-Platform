package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;
import vn.edu.fpt.petworldplatform.service.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
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
    public String getAllProducts(Model model, @RequestParam(name = "kw", required = false, defaultValue = "") String keyword) {

        if(!keyword.equals("")) {
            model.addAttribute("product", productService.searchProductsByName(keyword   ));
        }
        else {
            model.addAttribute("product", productService.getAllProducts());
        }
            return "/product/productList";

    }

    //Pet List
    @GetMapping("/pets")
    public String getAllPet(Model model) {
        model.addAttribute("pet", petService.getAllPets());
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
