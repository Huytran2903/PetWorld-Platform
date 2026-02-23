package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.petworldplatform.service.PetService;
//import vn.edu.fpt.petworldplatform.service.PetService;

@Controller
public class ProductController {

    @Autowired
    private PetService petService;

    //Product List
    @GetMapping("/products")
    public String showProductList(Model model) {
        model.addAttribute("formMode", "product");
        return "/product/petList";
    }

    //Pet List
    @GetMapping("/pets")
    public String getAllPet(Model model) {

        //model.addAttribute("pets", petService.getAllPets());
        model.addAttribute("formMode", "pet");
        return "petList";
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
