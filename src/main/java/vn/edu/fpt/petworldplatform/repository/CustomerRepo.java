package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Customer;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByUsername(String username);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    boolean existsById(Integer customerId);

    void deleteById(Integer customerId);

    Optional<Customer> findById(Integer customerId);
}
