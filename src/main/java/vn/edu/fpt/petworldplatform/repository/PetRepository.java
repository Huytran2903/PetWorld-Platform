package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Pet;
import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Integer> {

    List<Pet> findByNameContaining(String name);
}