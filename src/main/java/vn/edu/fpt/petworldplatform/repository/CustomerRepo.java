package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Customer;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);

    long countByIsActiveTrue();
}
