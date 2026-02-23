package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueRepository extends JpaRepository<Order, Integer> {

    // ============================================================
    // 1) Doanh thu hôm nay
    //    Dùng >= startOfDay AND < startOfNextDay thay CAST AS DATE
    // ============================================================
    @Query(value =
            "SELECT COALESCE(SUM(o.TotalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.Status IN ('paid', 'done') " +
            "  AND o.CreatedAt >= :startOfToday " +
            "  AND o.CreatedAt <  :startOfTomorrow",
            nativeQuery = true)
    BigDecimal getTodayRevenue(@Param("startOfToday")    LocalDateTime startOfToday,
                               @Param("startOfTomorrow") LocalDateTime startOfTomorrow);

    // ============================================================
    // 2) Doanh thu hôm qua (để tính % change)
    // ============================================================
    @Query(value =
            "SELECT COALESCE(SUM(o.TotalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.Status IN ('paid', 'done') " +
            "  AND o.CreatedAt >= :startOfYesterday " +
            "  AND o.CreatedAt <  :startOfToday",
            nativeQuery = true)
    BigDecimal getYesterdayRevenue(@Param("startOfYesterday") LocalDateTime startOfYesterday,
                                   @Param("startOfToday")     LocalDateTime startOfToday);

    // ============================================================
    // 3) Doanh thu tháng này
    //    Dùng DATEPART thay YEAR()/MONTH() (MySQL syntax, ko hợp SQL Server)
    // ============================================================
    @Query(value =
            "SELECT COALESCE(SUM(o.TotalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.Status IN ('paid', 'done') " +
            "  AND DATEPART(YEAR,  o.CreatedAt) = :year " +
            "  AND DATEPART(MONTH, o.CreatedAt) = :month",
            nativeQuery = true)
    BigDecimal getMonthlyRevenue(@Param("year")  int year,
                                 @Param("month") int month);

    // ============================================================
    // 4) Số đơn chờ xử lý
    // ============================================================
    @Query(value =
            "SELECT COUNT(*) FROM Orders o WHERE o.Status = 'pending'",
            nativeQuery = true)
    Long getPendingOrdersCount();

    // ============================================================
    // 5) Giao dịch gần đây (10 đơn mới nhất)
    //    JOIN Customers lấy FullName, LEFT JOIN Payments lấy Method
    //    Trả Object[] rồi map sang DTO ở Service
    // ============================================================
// Trong RevenueRepository — query này không filter theo Status cũng được để debug
@Query(value =
        "SELECT TOP 10 " +
        "  o.OrderCode, " +
        "  c.FullName, " +
        "  o.CreatedAt, " +
        "  o.TotalAmount, " +
        "  p.Method, " +
        "  o.Status " +
        "FROM Orders o " +
        "JOIN Customers c ON o.CustomerID = c.CustomerID " +
        "LEFT JOIN Payments p ON p.OrderID = o.OrderID " +
        "ORDER BY o.CreatedAt DESC",
        nativeQuery = true)
List<Object[]> getRecentTransactions();

    // ============================================================
    // 6) Filter theo date range
    //    Dùng COALESCE để handle null param
    // ============================================================
    @Query(value =
            "SELECT " +
            "  o.OrderCode, " +
            "  c.FullName, " +
            "  o.CreatedAt, " +
            "  o.TotalAmount, " +
            "  p.Method, " +
            "  o.Status " +
            "FROM Orders o " +
            "JOIN Customers c ON o.CustomerID = c.CustomerID " +
            "LEFT JOIN Payments p ON p.OrderID = o.OrderID " +
            "WHERE o.Status IN ('paid', 'done', 'pending') " +
            "  AND o.CreatedAt >= COALESCE(:startDate, '1900-01-01 00:00:00') " +
            "  AND o.CreatedAt <= COALESCE(:endDate,   '2099-12-31 23:59:59') " +
            "ORDER BY o.CreatedAt DESC",
            nativeQuery = true)
    List<Object[]> getOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate")   LocalDateTime endDate);
}