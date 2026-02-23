package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.petworldplatform.entity.Pets;
import java.util.List;

public interface PetRepo extends JpaRepository<Pets, Long> {

    List<Pets> findByNameContaining(String name);
    List<Pets> findByOwner_CustomerId(int customerId);
    @Query("SELECT count(p) FROM Pets p")
    long countTotalPets();

    @Query("SELECT p.petType, COUNT(p) FROM Pets p GROUP BY p.petType")
    List<Object[]> countPetsBySpecies();
}