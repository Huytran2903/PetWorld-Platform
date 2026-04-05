package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Product;
import vn.edu.fpt.petworldplatform.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;


    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable);
    }

    public Product findProductById(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
    }

    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    public void deleteById(Integer id) {
        productRepository.deleteById(id);
    }

    //search
    public Page<Product> searchProductsByName(String keyword, Pageable pageable) {
        return productRepository.searchAllByNameContainingIgnoreCase(keyword, pageable);
    }

    public List<Product> getRandomProducts(int limit) {
        return productRepository.findRandomProducts(limit);
    }

    public Page<Product> getProductsByCategory(Integer categoryId, Pageable pageable) {
        return productRepository.findByCategory_CategoryIDAndIsActiveTrue(categoryId, pageable);
    }

    public Page<Product> searchProductsByNameAndCategory(String keyword, Integer categoryId, Pageable pageable) {
        return productRepository.searchAllByNameContainingIgnoreCaseAndCategory_CategoryIDAndIsActiveTrue(keyword, categoryId, pageable);
    }


}
