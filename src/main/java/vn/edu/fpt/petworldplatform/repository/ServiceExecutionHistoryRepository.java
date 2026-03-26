package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceExecutionHistoryRepository extends JpaRepository<Appointment, Integer> {

    // ============================================================
    // Đếm tổng toàn thời gian
    // ============================================================
    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'done'", nativeQuery = true)
    Long getCompletedAppointmentsCount();

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'in_progress'", nativeQuery = true)
    Long getInProgressAppointmentsCount();

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'pending'", nativeQuery = true)
    Long getPendingAppointmentsCount();

    // ============================================================
    // Đếm theo khoảng ngày (dùng khi filter)
    // ============================================================
    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'done' " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getCompletedCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'in_progress' " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getInProgressCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'pending' " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getPendingCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // ============================================================
    // Đếm theo status (dùng khi filter chỉ có status)
    // ============================================================
    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'done' AND Status = :status", nativeQuery = true)
    Long getCompletedCountByStatus(@Param("status") String status);

    // ============================================================
    // Đếm theo status + khoảng ngày
    // ============================================================
    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'done' " +
                   "AND (:status IS NULL OR Status = :status) " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getCompletedCountByStatusAndDateRange(@Param("status") String status,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'in_progress' " +
                   "AND (:status IS NULL OR Status = :status) " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getInProgressCountByStatusAndDateRange(@Param("status") String status,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'pending' " +
                   "AND (:status IS NULL OR Status = :status) " +
                   "AND AppointmentDate >= :startDate AND AppointmentDate <= :endDate", nativeQuery = true)
    Long getPendingCountByStatusAndDateRange(@Param("status") String status,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // ============================================================
    // History queries
    // ============================================================
    @Query(value =
            "SELECT TOP 100 " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  (SELECT STRING_AGG(s2.Name, ', ') " +
            "     FROM AppointmentServices aps2 " +
            "     JOIN Services s2 ON aps2.ServiceID = s2.ServiceID " +
            "    WHERE aps2.AppointmentID = a.AppointmentID) AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  (SELECT STRING_AGG(x.FullName, ', ') " +
            "     FROM (SELECT DISTINCT st2.FullName " +
            "             FROM AppointmentServices aps3 " +
            "             JOIN Staff st2 ON aps3.AssignedStaffID = st2.StaffID " +
            "            WHERE aps3.AppointmentID = a.AppointmentID " +
            "              AND aps3.AssignedStaffID IS NOT NULL) x) AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getAllServiceExecutionHistory();

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  (SELECT STRING_AGG(s2.Name, ', ') " +
            "     FROM AppointmentServices aps2 " +
            "     JOIN Services s2 ON aps2.ServiceID = s2.ServiceID " +
            "    WHERE aps2.AppointmentID = a.AppointmentID) AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  (SELECT STRING_AGG(x.FullName, ', ') " +
            "     FROM (SELECT DISTINCT st2.FullName " +
            "             FROM AppointmentServices aps3 " +
            "             JOIN Staff st2 ON aps3.AssignedStaffID = st2.StaffID " +
            "            WHERE aps3.AppointmentID = a.AppointmentID " +
            "              AND aps3.AssignedStaffID IS NOT NULL) x) AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "WHERE a.Status = :status " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getServiceExecutionHistoryByStatus(@Param("status") String status);

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  (SELECT STRING_AGG(s2.Name, ', ') " +
            "     FROM AppointmentServices aps2 " +
            "     JOIN Services s2 ON aps2.ServiceID = s2.ServiceID " +
            "    WHERE aps2.AppointmentID = a.AppointmentID) AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  (SELECT STRING_AGG(x.FullName, ', ') " +
            "     FROM (SELECT DISTINCT st2.FullName " +
            "             FROM AppointmentServices aps3 " +
            "             JOIN Staff st2 ON aps3.AssignedStaffID = st2.StaffID " +
            "            WHERE aps3.AppointmentID = a.AppointmentID " +
            "              AND aps3.AssignedStaffID IS NOT NULL) x) AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "WHERE a.AppointmentDate >= COALESCE(:startDate, '1900-01-01 00:00:00') " +
            "  AND a.AppointmentDate <= COALESCE(:endDate, '2099-12-31 23:59:59') " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getServiceExecutionHistoryByDateRange(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  (SELECT STRING_AGG(s2.Name, ', ') " +
            "     FROM AppointmentServices aps2 " +
            "     JOIN Services s2 ON aps2.ServiceID = s2.ServiceID " +
            "    WHERE aps2.AppointmentID = a.AppointmentID) AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  (SELECT STRING_AGG(x.FullName, ', ') " +
            "     FROM (SELECT DISTINCT st2.FullName " +
            "             FROM AppointmentServices aps3 " +
            "             JOIN Staff st2 ON aps3.AssignedStaffID = st2.StaffID " +
            "            WHERE aps3.AppointmentID = a.AppointmentID " +
            "              AND aps3.AssignedStaffID IS NOT NULL) x) AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "WHERE a.Status = :status " +
            "  AND a.AppointmentDate >= COALESCE(:startDate, '1900-01-01 00:00:00') " +
            "  AND a.AppointmentDate <= COALESCE(:endDate, '2099-12-31 23:59:59') " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getServiceExecutionHistoryByStatusAndDateRange(@Param("status") String status,
                                                                   @Param("startDate") LocalDateTime startDate,
                                                                   @Param("endDate") LocalDateTime endDate);

    // ============================================================
    // Thống kê tần suất - TOÀN THỜI GIAN
    // ============================================================
    @Query(value =
            "SELECT " +
            "  s.Name AS ServiceName, " +
            "  ISNULL(COUNT(aps.AppointmentServiceID), 0) AS UsageCount, " +
            "  CASE " +
            "    WHEN SUM(COUNT(aps.AppointmentServiceID)) OVER() > 0 " +
            "    THEN CAST(COUNT(aps.AppointmentServiceID) * 100.0 / " +
            "              SUM(COUNT(aps.AppointmentServiceID)) OVER() AS DECIMAL(5,2)) " +
            "    ELSE 0 " +
            "  END AS Percentage " +
            "FROM Services s " +
            "LEFT JOIN AppointmentServices aps ON s.ServiceID = aps.ServiceID " +
            "LEFT JOIN Appointments a ON aps.AppointmentID = a.AppointmentID " +
            "                        AND a.Status IN ('done', 'in_progress') " +
            "WHERE s.IsActive = 1 " +
            "GROUP BY s.ServiceID, s.Name " +
            "ORDER BY UsageCount DESC",
            nativeQuery = true)
    List<Object[]> getServiceUsageStatistics();

    // ============================================================
    // Thống kê tần suất - CÓ FILTER NGÀY
    // ============================================================
    @Query(value =
            "SELECT " +
            "  s.Name AS ServiceName, " +
            "  COUNT(aps.AppointmentServiceID) AS UsageCount, " +
            "  CASE " +
            "    WHEN SUM(COUNT(aps.AppointmentServiceID)) OVER() > 0 " +
            "    THEN CAST(COUNT(aps.AppointmentServiceID) * 100.0 / " +
            "              SUM(COUNT(aps.AppointmentServiceID)) OVER() AS DECIMAL(5,2)) " +
            "    ELSE 0 " +
            "  END AS Percentage " +
            "FROM Services s " +
            "INNER JOIN AppointmentServices aps ON s.ServiceID = aps.ServiceID " +
            "INNER JOIN Appointments a ON aps.AppointmentID = a.AppointmentID " +
            "WHERE s.IsActive = 1 " +
            "  AND a.Status IN ('done', 'in_progress') " +
            "  AND a.AppointmentDate >= :startDate " +
            "  AND a.AppointmentDate <= :endDate " +
            "GROUP BY s.ServiceID, s.Name " +
            "ORDER BY UsageCount DESC",
            nativeQuery = true)
    List<Object[]> getServiceUsageStatisticsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
}