package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;

import java.util.Optional;

public interface AppointmentSummaryRepository extends JpaRepository<AppointmentSummary, Integer> {
    Optional<AppointmentSummary> findByAppointment_Id(Integer appointmentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE AppointmentSummaries SET SummaryByStaffID = NULL WHERE SummaryByStaffID = :oldStaffId", nativeQuery = true)
    void clearAllAppointmentSummary(@Param("oldStaffId") Integer oldStaffId);

}
