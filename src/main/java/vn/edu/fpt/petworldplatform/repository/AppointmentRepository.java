package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByCustomerIdOrderByAppointmentDateDesc(Integer customerId);

    List<Appointment> findByCustomerIdAndStatusInOrderByAppointmentDateDesc(Long customerId, List<String> statuses);

    /** Count appointments at same date/time (for double-booking check). */
    long countByAppointmentDateAndStatusNot(LocalDateTime appointmentDate, String excludedStatus);

    long countByAppointmentDateAndStatusNotIn(LocalDateTime appointmentDate, List<String> excludedStatuses);

    /** Detect overlapping appointments for a pet.
     * Overlap condition: existing.start < newEnd AND existing.end > newStart
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.petId = :petId " +
            "AND a.id != :excludeId " +
            "AND a.status NOT IN ('canceled', 'rejected') " +
            "AND a.appointmentDate < :newEnd " +
            "AND a.endTime > :newStart")
    long countOverlappingAppointments(@org.springframework.data.repository.query.Param("petId") Long petId,
                                      @org.springframework.data.repository.query.Param("excludeId") Integer excludeId,
                                      @org.springframework.data.repository.query.Param("newStart") java.time.LocalDateTime newStart,
                                      @org.springframework.data.repository.query.Param("newEnd") java.time.LocalDateTime newEnd);

    /** Detect overlapping appointments for a staff member. */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) FROM Appointment a " +
            "WHERE a.staffId = :staffId " +
            "AND a.id != :excludeId " +
            "AND a.status NOT IN ('canceled', 'rejected') " +
            "AND a.appointmentDate < :newEnd " +
            "AND a.endTime > :newStart")
    long countOverlappingStaffAppointments(@Param("staffId") Integer staffId,
                                            @Param("excludeId") Integer excludeId,
                                            @Param("newStart") LocalDateTime newStart,
                                            @Param("newEnd") LocalDateTime newEnd);
}
