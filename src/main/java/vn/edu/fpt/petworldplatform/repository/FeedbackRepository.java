package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.Feedback;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    // Lấy feedback theo trạng thái (vd: chỉ lấy pending)
    List<Feedback> findByStatus(String status);
    void deleteByStaff(Staff staff);
}