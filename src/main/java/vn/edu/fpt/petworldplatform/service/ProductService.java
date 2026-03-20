package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Product;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo productRepo;


    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepo.findByIsActiveTrue(pageable);
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

    //search
    public Page<Product> searchProductsByName(String keywork, Pageable pageable) {
        return productRepo.searchAllByNameContainingIgnoreCase(keywork, pageable);
    }

    public List<Product> getRandomProducts(int limit) {
        return productRepo.findRandomProducts(limit);
    }

    public Page<Product> getProductsByCategory(Integer categoryId, Pageable pageable) {
        return productRepo.findByCategory_CategoryIDAndIsActiveTrue(categoryId, pageable);
    }

    public Page<Product> searchProductsByNameAndCategory(String keyword, Integer categoryId, Pageable pageable) {
        return productRepo.searchAllByNameContainingIgnoreCaseAndCategory_CategoryIDAndIsActiveTrue(keyword, categoryId, pageable);
    }


}
