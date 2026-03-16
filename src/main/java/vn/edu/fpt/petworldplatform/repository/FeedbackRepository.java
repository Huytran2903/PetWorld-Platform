package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Feedback;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    // Lấy feedback theo trạng thái (vd: chỉ lấy pending)
    List<Feedback> findByStatus(String status);
    void deleteByStaff(Staff staff);

    boolean existsByAppointmentIdAndServiceIdAndCustomer_CustomerId(
            Integer appointmentId, Integer serviceId, Integer customerId);

    List<Feedback> findAllByOrderByCreatedAtDesc();

    List<Feedback> findByStatusOrderByCreatedAtDesc(String status);

    List<Feedback> findByTypeOrderByCreatedAtDesc(String type);

    List<Feedback> findByStatusAndTypeOrderByCreatedAtDesc(String status, String type);
}