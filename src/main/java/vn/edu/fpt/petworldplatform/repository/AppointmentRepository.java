package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByCustomerIdOrderByAppointmentDateDesc(Integer customerId);

    List<Appointment> findByCustomerIdAndStatusInOrderByAppointmentDateDesc(Integer customerId, List<String> statuses);

    long countByAppointmentDateAndStatusNot(LocalDateTime appointmentDate, String excludedStatus);

    long countByAppointmentDateAndStatusNotIn(LocalDateTime appointmentDate, List<String> excludedStatuses);

    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.petId = :petId " +
            "AND a.id != :excludeId " +
            "AND a.status NOT IN ('canceled', 'rejected') " +
            "AND a.appointmentDate < :newEnd " +
            "AND a.endTime > :newStart")
    long countOverlappingAppointments(@Param("petId") Integer petId,
                                      @Param("excludeId") Integer excludeId,
                                      @Param("newStart") LocalDateTime newStart,
                                      @Param("newEnd") LocalDateTime newEnd);

    @Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.staffId = :staffId " +
            "AND a.id != :excludeId " +
            "AND a.status NOT IN ('canceled', 'rejected') " +
            "AND a.appointmentDate < :newEnd " +
            "AND a.endTime > :newStart")
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
            "AND (cast(:from as date) IS NULL OR a.appointmentDate >= :from) " +
            "AND (cast(:to as date) IS NULL OR a.appointmentDate <= :to) " +
            "ORDER BY a.appointmentDate ASC")
    List<Appointment> findByAssignedStaffAndFilter(@Param("staffId") Long staffId,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("status") String status);

    @Query("SELECT a FROM Appointment a WHERE a.id = :appointmentId AND a.staffId = :staffId")
    Optional<Appointment> findByIdAndAssignedStaff(@Param("appointmentId") Integer appointmentId,
                                                   @Param("staffId") Long staffId);
}
