package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Pets;

import java.time.LocalDateTime;
import java.util.List;

public interface PetRepo extends JpaRepository<Pets, Integer> {

    List<Pets> findByNameContaining(String name);
    List<Pets> findByOwner_CustomerId(Integer customerId);

    //Oanh
    Page<Pets> findByOwnerIsNullAndPriceIsNotNull(Pageable pageable);



    // Pagination methods
    Page<Pets> findByOwner_CustomerId(Integer customerId, Pageable pageable);
    
    // Search with pagination
    Page<Pets> findByOwner_CustomerIdAndNameContainingIgnoreCase(Integer customerId, String name, Pageable pageable);
    
    // Filter by pet type with pagination
    Page<Pets> findByOwner_CustomerIdAndPetType(Integer customerId, String petType, Pageable pageable);
    
    // Search + filter with pagination
    Page<Pets> findByOwner_CustomerIdAndPetTypeAndNameContainingIgnoreCase(Integer customerId, String petType, String name, Pageable pageable);
    
    @Query("SELECT count(p) FROM Pets p WHERE p.owner.customerId = :customerId")
    long countByOwner_CustomerId(@Param("customerId") Integer customerId);
    
    @Query("SELECT count(p) FROM Pets p WHERE p.owner.customerId = :customerId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    long countByOwner_CustomerIdAndNameContainingIgnoreCase(@Param("customerId") Integer customerId, @Param("name") String name);
    
    @Query("SELECT count(p) FROM Pets p WHERE p.owner.customerId = :customerId AND p.petType = :petType")
    long countByOwner_CustomerIdAndPetType(@Param("customerId") Integer customerId, @Param("petType") String petType);
    
    @Query("SELECT count(p) FROM Pets p WHERE p.owner.customerId = :customerId AND p.petType = :petType AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    long countByOwner_CustomerIdAndPetTypeAndNameContainingIgnoreCase(@Param("customerId") Integer customerId, @Param("petType") String petType, @Param("name") String name);
    
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
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND (p.owner IS NOT NULL OR p.isAvailable = false )")
    long countServicePetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.price IS NOT NULL AND p.price > 0")
    long countSalePetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.purchasedAt IS NOT NULL")
    long countSoldPetsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Species-specific breakdowns
    @Query("SELECT 'SERVICE', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND (p.price IS NULL OR p.price = 0)")
    List<Object[]> countServicePetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT 'SERVICE', COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND (p.price IS NULL OR p.price = 0) AND (p.petType IS NULL OR (LOWER(p.petType) <> 'dog' AND LOWER(p.petType) <> 'cat'))")
    List<Object[]> countServicePetsByOtherSpeciesAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT 'SALE', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND p.price IS NOT NULL AND p.price > 0")
    List<Object[]> countSalePetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT 'SALE', COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.price IS NOT NULL AND p.price > 0 AND (p.petType IS NULL OR (LOWER(p.petType) <> 'dog' AND LOWER(p.petType) <> 'cat'))")
    List<Object[]> countSalePetsByOtherSpeciesAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT 'SOLD', COUNT(p) FROM Pets p WHERE p.petType = :species AND p.createdAt BETWEEN :startDate AND :endDate AND p.purchasedAt IS NOT NULL")
    List<Object[]> countSoldPetsBySpeciesAndDateRange(@Param("species") String species, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT 'SOLD', COUNT(p) FROM Pets p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.purchasedAt IS NOT NULL AND (p.petType IS NULL OR (LOWER(p.petType) <> 'dog' AND LOWER(p.petType) <> 'cat'))")
    List<Object[]> countSoldPetsByOtherSpeciesAndDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    //List<Pets> searchAllByNameContainingIgnoreCase(String keyword);

    Page<Pets> findAllByOwnerIsNullAndPriceIsNotNull(Pageable pageable);

    Page<Pets> findAllByPetTypeIgnoreCase(String type, Pageable pageable);

    Page<Pets> findAllByNameContainingIgnoreCaseAndPetTypeIgnoreCaseAndOwnerIsNullAndPriceIsNotNull(
            String name, String petType, Pageable pageable
    );

    Page<Pets> findAllByNameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Pets> findAllByOwnerIsNull( String status, Pageable pageable);
    Page<Pets> findAllByOwnerIsNotNull(String status, Pageable pageable);
}
