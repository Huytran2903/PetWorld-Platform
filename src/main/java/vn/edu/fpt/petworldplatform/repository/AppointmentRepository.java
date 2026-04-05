package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByCustomerIdOrderByAppointmentDateDesc(Integer customerId);

        List<Appointment> findByCustomerIdAndPetIdOrderByAppointmentDateDesc(Integer customerId, Integer petId);

    List<Appointment> findByCustomerIdAndStatusInOrderByAppointmentDateDesc(Integer customerId, List<String> statuses);

    // Pending first, then all others ordered by appointment date (newest first).
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.customerId = :customerId " +
            "ORDER BY CASE WHEN lower(a.status) = 'pending' THEN 0 ELSE 1 END, a.appointmentDate DESC")
    Page<Appointment> findCustomerAppointmentsPendingFirst(@Param("customerId") Integer customerId, Pageable pageable);

    long countByAppointmentDateAndStatusNot(LocalDateTime appointmentDate, String excludedStatus);

    long countByAppointmentDateAndStatusNotIn(LocalDateTime appointmentDate, List<String> excludedStatuses);

    /**
     * Legacy rows may have NULL EndTime; treat as start + 60 minutes (same as {@code effectiveOverlapEnd}).
     * Status compared case-insensitively; British spelling {@code cancelled} excluded too.
     */
    @Query(value = "SELECT COUNT(*) FROM Appointments a WHERE a.PetID = :petId "
            + "AND a.AppointmentID <> :excludeId "
            + "AND LOWER(LTRIM(RTRIM(a.Status))) NOT IN ('canceled', 'cancelled', 'rejected') "
            + "AND a.AppointmentDate < :newEnd "
            + "AND COALESCE(a.EndTime, DATEADD(MINUTE, 60, a.AppointmentDate)) > :newStart",
            nativeQuery = true)
    long countOverlappingAppointments(@Param("petId") Integer petId,
                                      @Param("excludeId") Integer excludeId,
                                      @Param("newStart") LocalDateTime newStart,
                                      @Param("newEnd") LocalDateTime newEnd);

    @Query(value = "SELECT COUNT(*) FROM Appointments a WHERE a.StaffID = :staffId "
            + "AND a.AppointmentID <> :excludeId "
            + "AND LOWER(LTRIM(RTRIM(a.Status))) NOT IN ('canceled', 'cancelled', 'rejected') "
            + "AND a.AppointmentDate < :newEnd "
            + "AND COALESCE(a.EndTime, DATEADD(MINUTE, 60, a.AppointmentDate)) > :newStart",
            nativeQuery = true)
    long countOverlappingStaffAppointments(@Param("staffId") Integer staffId,
                                           @Param("excludeId") Integer excludeId,
                                           @Param("newStart") LocalDateTime newStart,
                                           @Param("newEnd") LocalDateTime newEnd);

    List<Appointment> findByStaffIdOrderByAppointmentDateDesc(Integer staffId);

    @Query("SELECT a FROM Appointment a WHERE a.staffId = :staffId ORDER BY a.appointmentDate ASC")
    List<Appointment> findAssignedToStaff(@Param("staffId") Integer staffId);

    List<Appointment> findByStaffIdAndStatusOrderByAppointmentDateDesc(Integer staffId, String status);

    @Query("SELECT a FROM Appointment a WHERE a.staffId = :staffId " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:from IS NULL OR a.appointmentDate >= :from) " +
            "AND (:to IS NULL OR a.appointmentDate <= :to) " +
            "ORDER BY a.appointmentDate ASC")
    List<Appointment> findByAssignedStaffAndFilter(@Param("staffId") Integer staffId,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("status") String status);

    @Query("SELECT a FROM Appointment a WHERE a.id = :appointmentId AND a.staffId = :staffId")
    Optional<Appointment> findByIdAndAssignedStaff(@Param("appointmentId") Integer appointmentId,
                                                   @Param("staffId") Integer staffId);

    /**
     * Customer self-service anti-spam: count canceled appointments since {@code since} (uses CanceledAt).
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.customerId = :customerId " +
            "AND LOWER(TRIM(a.status)) IN ('canceled', 'cancelled') " +
            "AND a.canceledAt IS NOT NULL AND a.canceledAt >= :since")
    long countCustomerCancellationsSince(@Param("customerId") Integer customerId, @Param("since") LocalDateTime since);

    /**
     * Counts no-show appointments for this customer whose scheduled time falls on or after {@code since}
     * (rolling window by appointment slot).
     */
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.customerId = :customerId "
            + "AND LOWER(TRIM(a.status)) = 'no_show' "
            + "AND a.appointmentDate >= :since")
    long countCustomerNoShowsSince(@Param("customerId") Integer customerId, @Param("since") LocalDateTime since);

    @Modifying
    @Query(value = "UPDATE Appointments SET StaffID = :newStaffId WHERE StaffID = :oldStaffId AND Status IN ('Pending', 'Confirmed', 'Pending')", nativeQuery = true)
    void transferPendingAppointments(@Param("oldStaffId") Integer oldStaffId, @Param("newStaffId") Integer newStaffId);

    @Modifying
    @Query(value = "UPDATE Appointments SET StaffID = NULL WHERE StaffID = :oldStaffId AND Status IN ('Pending', 'Confirmed')", nativeQuery = true)
    void unassignPendingAppointments(@Param("oldStaffId") Integer oldStaffId);

    @Modifying
    @Query(value = "UPDATE Appointments SET StaffID = NULL WHERE StaffID = :oldStaffId", nativeQuery = true)
    void clearAllStaffReferences(@Param("oldStaffId") Integer oldStaffId);
}
