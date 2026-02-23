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
    // 4) Doanh thu toàn thời gian
    // ============================================================
    @Query(value =
            "SELECT COALESCE(SUM(o.TotalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.Status IN ('paid', 'done')",
            nativeQuery = true)
    BigDecimal getAllTimeRevenue();

    // ============================================================
    // 5) Doanh thu theo date range
    // ============================================================
    @Query(value =
            "SELECT COALESCE(SUM(o.TotalAmount), 0) " +
            "FROM Orders o " +
            "WHERE o.Status IN ('paid', 'done') " +
            "  AND o.CreatedAt >= :startDate " +
            "  AND o.CreatedAt <= :endDate",
            nativeQuery = true)
    BigDecimal getRevenueByDateRange(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate")   LocalDateTime endDate);

    // ============================================================
    // 6) Số đơn pending toàn hệ thống
    // ============================================================
    @Query(value =
            "SELECT COUNT(*) FROM Orders o WHERE o.Status = 'pending'",
            nativeQuery = true)
    Long getPendingOrdersCount();

    // ============================================================
    // 7) Số đơn pending theo date range
    // ============================================================
    @Query(value =
            "SELECT COUNT(*) FROM Orders o " +
            "WHERE o.Status = 'pending' " +
            "  AND o.CreatedAt >= :startDate " +
            "  AND o.CreatedAt <= :endDate",
            nativeQuery = true)
    Long getPendingOrdersCountByDateRange(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate")   LocalDateTime endDate);

    // ============================================================
    // 8) Tất cả giao dịch (không giới hạn)
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
            "ORDER BY o.CreatedAt DESC",
            nativeQuery = true)
    List<Object[]> getAllTransactions();

    // ============================================================
    // 9) Giao dịch theo date range
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
            "  AND o.CreatedAt >= :startDate " +
            "  AND o.CreatedAt <= :endDate " +
            "ORDER BY o.CreatedAt DESC",
            nativeQuery = true)
    List<Object[]> getOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate")   LocalDateTime endDate);
}