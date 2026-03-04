package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.service.PetService;
import vn.edu.fpt.petworldplatform.service.ProductService;
//import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
public class ProductController {

    @Autowired
    private PetService petService;

    @Autowired
    private ProductService productService;

    //Product List
    @GetMapping("/products")
    public String getAllProducts(Model model, @RequestParam(name = "kw", required = false, defaultValue = "") String keywork) {

        if(!keywork.equals("")) {
            model.addAttribute("product", productService.searchProductsByName(keywork));
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
