package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Product;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Integer> {

    boolean existsByCategory_CategoryID(Integer categoryID);

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Page<Product> searchAllByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query(value = "SELECT TOP (:limit) * FROM products ORDER BY NEWID()", nativeQuery = true)
    List<Product> findRandomProducts(@Param("limit") int limit);

    Page<Product> findByCategory_CategoryIDAndIsActiveTrue(Integer categoryId, Pageable pageable);

    // 2. Tìm theo Tên (chứa từ khóa) + ID danh mục + sản phẩm đang Active
    Page<Product> searchAllByNameContainingIgnoreCaseAndCategory_CategoryIDAndIsActiveTrue(String name, Integer categoryId, Pageable pageable);
}
