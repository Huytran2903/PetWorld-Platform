package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentServiceLineRepository extends JpaRepository<AppointmentServiceLine, Integer> {

    List<AppointmentServiceLine> findByAssignedStaff(Staff staff);

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

    // 1. Chuyển giao dịch vụ (Bọc TRIM và LOWER để diệt khoảng trắng thừa)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE AppointmentServices SET AssignedStaffID = :newStaffId WHERE AssignedStaffID = :oldStaffId AND LTRIM(RTRIM(LOWER(ServiceStatus))) IN ('pending', 'assigned', 'in_progress')", nativeQuery = true)
    void transferPendingServices(@Param("oldStaffId") Integer oldStaffId, @Param("newStaffId") Integer newStaffId);

    // 2. Trả về vô chủ (NULL) nếu không bàn giao
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE AppointmentServices SET AssignedStaffID = NULL, ServiceStatus = 'pending' WHERE AssignedStaffID = :oldStaffId AND LTRIM(RTRIM(LOWER(ServiceStatus))) IN ('pending', 'assigned', 'in_progress')", nativeQuery = true)
    void unassignPendingServices(@Param("oldStaffId") Integer oldStaffId);

    // 3. Quét sạch dấu vết quá khứ
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE AppointmentServices SET AssignedStaffID = NULL WHERE AssignedStaffID = :oldStaffId", nativeQuery = true)
    void clearAllStaffReferences(@Param("oldStaffId") Integer oldStaffId);

    // Đếm In-Progress
    @Query("SELECT COUNT(a) FROM AppointmentServiceLine a WHERE a.assignedStaff.staffId = :staffId AND a.serviceStatus = 'in_progress'")
    long countInProgressServices(@Param("staffId") Integer staffId);
}
