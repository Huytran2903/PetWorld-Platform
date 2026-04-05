package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Categories;
import vn.edu.fpt.petworldplatform.repository.CategoryRepository;
import vn.edu.fpt.petworldplatform.repository.ProductRepository;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

        public List<Categories> getAllCategories()  {
        return categoryRepository.findAll();
    }

    public Categories getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
    }

    public void saveCategory(Categories cate) {
        categoryRepository.save(cate);
    }

    public void deleteCategoryById(Integer id) {
        categoryRepository.deleteById(id);
    }

    public boolean hasProducts(Integer categoryId) {
        return productRepository.existsByCategory_CategoryID(categoryId);
    }

    public List<Categories> searchCateByName(String keyword) {
        return categoryRepository.findAllByNameContainingIgnoreCase(keyword);
    }

}
