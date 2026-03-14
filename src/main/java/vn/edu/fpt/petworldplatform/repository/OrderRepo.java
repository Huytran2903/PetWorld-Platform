package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Order;

@Repository
public interface OrderRepo extends JpaRepository<Order, Integer> {
    Order findByOrderCode(String orderCode);
}
