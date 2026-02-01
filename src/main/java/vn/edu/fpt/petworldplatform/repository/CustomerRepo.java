package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Customer;

import java.util.Optional;

@Repository
public interface CustomerRepo extends JpaRepository<Customer, Long> {
    // Tìm kiếm khách hàng theo username để phục vụ Login
    Optional<Customer> findByUsername(String username);

    // Kiểm tra xem email đã tồn tại chưa khi Register
    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

}
