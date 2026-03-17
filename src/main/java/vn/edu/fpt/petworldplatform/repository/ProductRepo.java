package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Product;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Integer> {

    boolean existsByCategory_CategoryID(Integer categoryID);

    List<Product> searchAllByNameContainingIgnoreCase(String name);

    @Query(value = "SELECT TOP (:limit) * FROM products ORDER BY NEWID()", nativeQuery = true)
    List<Product> findRandomProducts(@Param("limit") int limit);
}
