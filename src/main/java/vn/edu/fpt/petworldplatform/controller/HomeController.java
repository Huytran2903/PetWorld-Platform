package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.petworldplatform.entity.Product;
import vn.edu.fpt.petworldplatform.service.ProductService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String homePage(Model model) {
        try {
            List<Product> randomProducts = productService.getRandomProducts(8);
            model.addAttribute("products", randomProducts);
        } catch (Exception e) {
            model.addAttribute("products", new ArrayList<>());
        }
        return "home/home";

    }

    @GetMapping("/about-us")
    public String aboutUs(){
        return "customer/about-us";
    }
}
