package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
	long countByCustomer_CustomerIdAndIsReadFalse(Integer customerId);

	List<Notification> findTop5ByCustomer_CustomerIdOrderByCreatedAtDesc(Integer customerId);

	Page<Notification> findByCustomer_CustomerId(Integer customerId, Pageable pageable);

	Optional<Notification> findByIdAndCustomer_CustomerId(Integer id, Integer customerId);
}
