package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Pets;
import java.util.List;

public interface PetRepo extends JpaRepository<Pets, Long> {

    List<Pets> findByNameContaining(String name);
    List<Pets> findByOwner_CustomerId(Long customerId);

}