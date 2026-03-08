package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentServiceLineRepository extends JpaRepository<AppointmentServiceLine, Integer> {

    List<AppointmentServiceLine> findByAppointment_Id(Integer appointmentId);

    @Query("SELECT asl FROM AppointmentServiceLine asl JOIN FETCH asl.service s WHERE asl.appointment.id IN :appointmentIds")
    List<AppointmentServiceLine> findAllByAppointmentIdsWithService(@Param("appointmentIds") List<Integer> appointmentIds);

    @Query("SELECT asl FROM AppointmentServiceLine asl " +
            "JOIN FETCH asl.appointment a " +
            "JOIN FETCH a.pet p " +
            "JOIN FETCH a.customer c " +
            "JOIN FETCH asl.service s " +
            "LEFT JOIN FETCH asl.assignedStaff staff " +
            "WHERE asl.assignedStaffId = :staffId " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (cast(:from as date) IS NULL OR a.appointmentDate >= :from) " +
            "AND (cast(:to as date) IS NULL OR a.appointmentDate <= :to) " +
            "ORDER BY a.appointmentDate ASC, asl.id ASC")
    List<AppointmentServiceLine> findAssignedLinesByStaffAndFilter(@Param("staffId") Integer staffId,
                                                                   @Param("from") LocalDateTime from,
                                                                   @Param("to") LocalDateTime to,
                                                                   @Param("status") String status);

    @Query("SELECT asl FROM AppointmentServiceLine asl " +
            "JOIN FETCH asl.appointment a " +
            "JOIN FETCH a.pet p " +
            "JOIN FETCH a.customer c " +
            "JOIN FETCH asl.service s " +
            "LEFT JOIN FETCH asl.assignedStaff staff " +
            "WHERE asl.id = :lineId AND asl.assignedStaffId = :staffId")
    Optional<AppointmentServiceLine> findDetailByIdAndAssignedStaff(@Param("lineId") Integer lineId,
                                                                    @Param("staffId") Integer staffId);

    List<AppointmentServiceLine> findByAppointment_IdAndAssignedStaffId(Integer appointmentId, Integer staffId);

    List<AppointmentServiceLine> findByAppointment_IdAndAssignedStaffIdOrderByIdAsc(Integer appointmentId, Integer staffId);

    @Query("SELECT COUNT(asl) FROM AppointmentServiceLine asl " +
            "WHERE asl.assignedStaffId = :staffId " +
            "AND asl.appointment.id <> :appointmentId " +
            "AND asl.appointment.status NOT IN ('canceled', 'rejected') " +
            "AND asl.appointment.appointmentDate < :newEnd " +
            "AND asl.appointment.endTime > :newStart")
    long countOverlappingAssignedLines(@Param("staffId") Integer staffId,
                                       @Param("appointmentId") Integer appointmentId,
                                       @Param("newStart") LocalDateTime newStart,
                                       @Param("newEnd") LocalDateTime newEnd);

    @Modifying
    @Query("UPDATE AppointmentServiceLine asl SET asl.assignedStaff = :staff WHERE asl.id = :lineId")
    void assignStaffToLine(@Param("lineId") Integer lineId, @Param("staff") vn.edu.fpt.petworldplatform.entity.Staff staff);

    long countByAppointment_IdAndAssignedStaffIdIsNotNull(Integer appointmentId);

    long countByAppointment_Id(Integer appointmentId);

    // delete all lines belonging to an appointment
    void deleteAllByAppointment(Appointment appointment);
}
