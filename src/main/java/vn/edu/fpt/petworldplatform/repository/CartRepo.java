package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Carts;

import java.util.Optional;

@Repository
public interface CartRepo extends JpaRepository<Carts, Integer> {
    Optional<Carts> findByCustomerId(Integer customerId);

    void deleteByCustomerId(Integer customerId);
}
