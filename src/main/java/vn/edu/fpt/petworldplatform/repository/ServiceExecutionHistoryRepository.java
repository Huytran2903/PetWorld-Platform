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

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'done'", nativeQuery = true)
    Long getCompletedAppointmentsCount();

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'in_progress'", nativeQuery = true)
    Long getInProgressAppointmentsCount();

    @Query(value = "SELECT COUNT(*) FROM Appointments WHERE Status = 'pending'", nativeQuery = true)
    Long getPendingAppointmentsCount();

    @Query(value =
            "SELECT TOP 100 " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  STRING_AGG(s.Name, ', ') AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  STRING_AGG(st.FullName, ', ') AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "LEFT JOIN AppointmentServices aps ON a.AppointmentID = aps.AppointmentID " +
            "LEFT JOIN Services s ON aps.ServiceID = s.ServiceID " +
            "LEFT JOIN AppointmentAssignments aa ON a.AppointmentID = aa.AppointmentID " +
            "LEFT JOIN Staff st ON aa.StaffID = st.StaffID " +
            "GROUP BY a.AppointmentID, a.AppointmentCode, c.FullName, p.Name, a.AppointmentDate, a.Status " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getAllServiceExecutionHistory();

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  STRING_AGG(s.Name, ', ') AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  STRING_AGG(st.FullName, ', ') AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "LEFT JOIN AppointmentServices aps ON a.AppointmentID = aps.AppointmentID " +
            "LEFT JOIN Services s ON aps.ServiceID = s.ServiceID " +
            "LEFT JOIN AppointmentAssignments aa ON a.AppointmentID = aa.AppointmentID " +
            "LEFT JOIN Staff st ON aa.StaffID = st.StaffID " +
            "WHERE a.Status = :status " +
            "GROUP BY a.AppointmentID, a.AppointmentCode, c.FullName, p.Name, a.AppointmentDate, a.Status " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getServiceExecutionHistoryByStatus(@Param("status") String status);

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  STRING_AGG(s.Name, ', ') AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  STRING_AGG(st.FullName, ', ') AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "LEFT JOIN AppointmentServices aps ON a.AppointmentID = aps.AppointmentID " +
            "LEFT JOIN Services s ON aps.ServiceID = s.ServiceID " +
            "LEFT JOIN AppointmentAssignments aa ON a.AppointmentID = aa.AppointmentID " +
            "LEFT JOIN Staff st ON aa.StaffID = st.StaffID " +
            "WHERE a.AppointmentDate >= COALESCE(:startDate, '1900-01-01 00:00:00') " +
            "  AND a.AppointmentDate <= COALESCE(:endDate, '2099-12-31 23:59:59') " +
            "GROUP BY a.AppointmentID, a.AppointmentCode, c.FullName, p.Name, a.AppointmentDate, a.Status " +
            "ORDER BY a.AppointmentDate DESC",
            nativeQuery = true)
    List<Object[]> getServiceExecutionHistoryByDateRange(@Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);

    @Query(value =
            "SELECT " +
            "  a.AppointmentCode, " +
            "  c.FullName AS CustomerName, " +
            "  p.Name AS PetName, " +
            "  STRING_AGG(s.Name, ', ') AS ServiceNames, " +
            "  a.AppointmentDate, " +
            "  a.Status, " +
            "  STRING_AGG(st.FullName, ', ') AS AssignedStaff " +
            "FROM Appointments a " +
            "JOIN Customers c ON a.CustomerID = c.CustomerID " +
            "JOIN Pets p ON a.PetID = p.PetID " +
            "LEFT JOIN AppointmentServices aps ON a.AppointmentID = aps.AppointmentID " +
            "LEFT JOIN Services s ON aps.ServiceID = s.ServiceID " +
            "LEFT JOIN AppointmentAssignments aa ON a.AppointmentID = aa.AppointmentID " +
            "LEFT JOIN Staff st ON aa.StaffID = st.StaffID " +
            "WHERE a.Status = :status " +
            "  AND a.AppointmentDate >= COALESCE(:startDate, '1900-01-01 00:00:00') " +
            "  AND a.AppointmentDate <= COALESCE(:endDate, '2099-12-31 23:59:59') " +
            "GROUP BY a.AppointmentID, a.AppointmentCode, c.FullName, p.Name, a.AppointmentDate, a.Status " +
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
    // FIX: Thống kê tần suất - CÓ FILTER NGÀY
    // Điều kiện lọc ngày đặt trong WHERE (không phải JOIN)
    // để SQL thực sự loại bỏ các appointment ngoài khoảng ngày
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