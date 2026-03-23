package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Categories;

import java.util.List;

public interface CategoryRepo extends JpaRepository<Categories, Integer> {

    List<Categories> findAllByNameContainingIgnoreCase(String keyword);

}
