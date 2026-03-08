package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Pets;
import java.time.LocalDateTime;
import java.util.List;

public interface PetRepo extends JpaRepository<Pets, Integer> {

    List<Pets> findByNameContaining(String name);
    List<Pets> findByOwner_CustomerId(Integer customerId);
    
    @Query("SELECT count(p) FROM Pets p")
    long countTotalPets();

    @Query("SELECT p.petType, COUNT(p) FROM Pets p GROUP BY p.petType")
    List<Object[]> countPetsBySpecies();
    
    // Time-based statistics
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    long countPetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT p.petType, COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate GROUP BY p.petType")
    List<Object[]> countPetsBySpeciesAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Service vs Sale breakdown
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND (p.price IS NULL OR p.price = 0)")
    long countServicePetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.price IS NOT NULL AND p.price > 0")
    long countSalePetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.purchasedAt IS NOT NULL")
    long countSoldPetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Species-specific breakdowns
    @Query("SELECT 'SERVICE', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND (p.price IS NULL OR p.price = 0)")
    List<Object[]> countServicePetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT 'SALE', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND p.price IS NOT NULL AND p.price > 0")
    List<Object[]> countSalePetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT 'SOLD', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND p.purchasedAt IS NOT NULL")
    List<Object[]> countSoldPetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
