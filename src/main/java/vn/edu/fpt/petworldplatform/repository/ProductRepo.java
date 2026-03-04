package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Product;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Integer> {

    boolean existsByCategory_CategoryID(Integer categoryID);

    List<Product> searchAllByNameContainingIgnoreCase(String name);
}
