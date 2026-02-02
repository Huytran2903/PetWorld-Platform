package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    
    @Query(value = "SELECT * FROM Payments WHERE OrderID = :orderId AND PaymentType = 'order'", nativeQuery = true)
    Payment findByOrderId(@Param("orderId") Integer orderId);
}