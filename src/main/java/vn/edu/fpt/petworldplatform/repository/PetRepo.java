package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Pets;

public interface PetRepo extends JpaRepository<Pets, Long> {
}
