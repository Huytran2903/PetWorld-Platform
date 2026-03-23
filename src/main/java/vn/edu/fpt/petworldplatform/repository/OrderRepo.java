package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Order;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepo extends JpaRepository<Order, Integer> {
    Order findByOrderCode(String orderCode);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'COMPLETED'")
    Double sumTotalRevenue();

    List<Order> findTop5ByOrderByCreatedAtDesc();

    Page<Order> findByCreatedAtBetweenAndStatus(LocalDateTime start, LocalDateTime end, String status, Pageable pageable);

    Page<Order> findByCreatedAtBetween (LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);
}
