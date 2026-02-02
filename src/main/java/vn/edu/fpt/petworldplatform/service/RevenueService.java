package vn.edu.fpt.petworldplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.dto.RevenueDTO;
import vn.edu.fpt.petworldplatform.repository.RevenueRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RevenueService {

    private final RevenueRepository repo;

    public RevenueService(RevenueRepository repo) {
        this.repo = repo;
    }

    // ============================================================
    // Doanh thu hôm nay
    // ============================================================
    public BigDecimal getTodayRevenue() {
        LocalDateTime startOfToday    = LocalDate.now().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);
        return repo.getTodayRevenue(startOfToday, startOfTomorrow);
    }

    // ============================================================
    // % thay đổi so với hôm qua
    // ============================================================
    public double getPercentChange() {
        LocalDateTime startOfToday    = LocalDate.now().atStartOfDay();
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime startOfTomorrow = startOfToday.plusDays(1);

        BigDecimal today     = repo.getTodayRevenue(startOfToday, startOfTomorrow);
        BigDecimal yesterday = repo.getYesterdayRevenue(startOfYesterday, startOfToday);

        if (yesterday.compareTo(BigDecimal.ZERO) == 0) {
            return today.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        // (today - yesterday) / yesterday * 100
        return today.subtract(yesterday)
                    .divide(yesterday, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
    }

    // ============================================================
    // Doanh thu tháng này
    // ============================================================
    public BigDecimal getMonthlyRevenue() {
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        return repo.getMonthlyRevenue(year, month);
    }

    // ============================================================
    // Số đơn chờ
    // ============================================================
    public Long getPendingOrdersCount() {
        return repo.getPendingOrdersCount();
    }

    // ============================================================
    // Giao dịch gần đây (không filter)
    // ============================================================
    public List<RevenueDTO> getRecentTransactions() {
        List<Object[]> rows = repo.getRecentTransactions();
        return mapToDTO(rows);
    }

    // ============================================================
    // Giao dịch theo date range (có filter)
    // ============================================================
    public List<RevenueDTO> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rows = repo.getOrdersByDateRange(startDate, endDate);
        return mapToDTO(rows);
    }

    // ============================================================
    // Map Object[] -> RevenueDTO
    // Thứ tự columns phải khớp với SELECT trong query:
    //   [0] OrderCode, [1] FullName, [2] CreatedAt,
    //   [3] TotalAmount, [4] Method, [5] Status
    // ============================================================
    private List<RevenueDTO> mapToDTO(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String orderCode     = row[0] != null ? row[0].toString() : "";
            String customerName  = row[1] != null ? row[1].toString() : "";

            // row[2] có thể là Timestamp hoặc LocalDateTime tùy JDBC driver
            LocalDateTime orderDate = null;
            if (row[2] != null) {
                if (row[2] instanceof java.sql.Timestamp) {
                    orderDate = ((java.sql.Timestamp) row[2]).toLocalDateTime();
                } else if (row[2] instanceof LocalDateTime) {
                    orderDate = (LocalDateTime) row[2];
                }
            }

            BigDecimal totalAmount = row[3] != null
                    ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            String paymentMethod = row[4] != null ? row[4].toString() : null;
            String status        = row[5] != null ? row[5].toString() : "";

            return new RevenueDTO(orderCode, customerName, orderDate,
                                  totalAmount, status, paymentMethod);
        }).collect(Collectors.toList());
    }
}