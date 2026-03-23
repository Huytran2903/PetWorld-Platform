package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.entity.StaffSchedule;

import java.time.LocalDate;
import java.util.List;

public interface StaffScheduleRepository extends JpaRepository<StaffSchedule, Integer> {
    List<StaffSchedule> findByStaff_StaffIdAndWorkDateBetweenOrderByWorkDateAscStartTimeAsc(Integer staffId, LocalDate startDate, LocalDate endDate);

    @Query("""
            SELECT ss
            FROM StaffSchedule ss
            WHERE ss.staff.staffId = :staffId
              AND ss.workDate BETWEEN :startDate AND :endDate
            ORDER BY ss.workDate ASC, ss.startTime ASC
            """)
    List<StaffSchedule> findScheduleByStaffAndDateRange(@Param("staffId") Integer staffId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    void deleteByStaff(Staff staff);
}
