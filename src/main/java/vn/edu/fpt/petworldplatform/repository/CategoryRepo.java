package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Categories;

public interface CategoryRepo extends JpaRepository<Categories, Integer> {


}
