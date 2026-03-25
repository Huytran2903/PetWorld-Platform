package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;
import java.util.Optional;

public interface PetHealthRecordRepository extends JpaRepository<PetHealthRecord, Integer> {
    Optional<PetHealthRecord> findTopByAppointment_IdOrderByUpdatedAtDesc(Integer appointmentId);

    List<PetHealthRecord> findByAppointment_Id(Integer appointmentId);

    Optional<PetHealthRecord> findByAppointment_IdAndAppointmentServiceLine_Id(Integer appointmentId, Integer serviceLineId);

    Optional<PetHealthRecord> findByAppointmentServiceLine_Id(Integer serviceLineId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE PetHealthRecords SET PerformedByStaffID = NULL WHERE PerformedByStaffID = :oldStaffId", nativeQuery = true)
    void unassignAllHealthRecords(@Param("oldStaffId") Integer oldStaffId);

    @Query("SELECT COUNT(h) FROM PetHealthRecord h WHERE h.performedByStaff.staffId = :staffId AND h.isDraft = true AND h.isDeleted = false")
    long countPendingHealthRecords(@Param("staffId") Integer staffId);

}
