package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Categories;
import vn.edu.fpt.petworldplatform.repository.CategoryRepo;
import vn.edu.fpt.petworldplatform.repository.ProductRepo;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private ProductRepo productRepo;

        public List<Categories> getAllCategories()  {
        return categoryRepo.findAll();
    }

    public Categories getCategoryById(Integer id) {
        return categoryRepo.findById(id).get();
    }

    public void saveCategory(Categories cate) {
        categoryRepo.save(cate);
    }

    public void deleteCategoryById(Integer id) {
        categoryRepo.deleteById(id);
    }

    public boolean hasProducts(Integer categoryId) {
        return productRepo.existsByCategory_CategoryID(categoryId);
    }

    public List<Categories> searchCateByName(String keyword) {
        return categoryRepo.findAllByNameContainingIgnoreCase(keyword);
    }

}
