package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.petworldplatform.entity.StaffSchedule;

import java.time.LocalDate;
import java.util.List;

public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Integer> {
    List<StaffSchedule> findByStaffIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(Long staffId, LocalDate startDate, LocalDate endDate);
}
