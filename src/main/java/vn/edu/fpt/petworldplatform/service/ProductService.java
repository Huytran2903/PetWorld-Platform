package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Product;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;


    public List<Product> getAllProducts() {
        return productRepo.findAll();
    }

    public Product getProductById(Integer id) {
        return productRepo.findById(id).get();
    }

    public void saveProduct(Product product) {
        productRepo.save(product);
    }

    public void deleteById(Integer id) {
        productRepo.deleteById(id);
    }




}
