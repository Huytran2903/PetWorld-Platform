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
        LocalDateTime startOfToday     = LocalDate.now().atStartOfDay();
        LocalDateTime startOfYesterday = startOfToday.minusDays(1);
        LocalDateTime startOfTomorrow  = startOfToday.plusDays(1);

        BigDecimal today     = repo.getTodayRevenue(startOfToday, startOfTomorrow);
        BigDecimal yesterday = repo.getYesterdayRevenue(startOfYesterday, startOfToday);

        if (yesterday.compareTo(BigDecimal.ZERO) == 0) {
            return today.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
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
    // Doanh thu toàn thời gian
    // ============================================================
    public BigDecimal getAllTimeRevenue() {
        return repo.getAllTimeRevenue();
    }

    // ============================================================
    // Doanh thu theo date range
    // ============================================================
    public BigDecimal getRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return repo.getRevenueByDateRange(startDate, endDate);
    }

    // ============================================================
    // Số đơn pending toàn hệ thống
    // ============================================================
    public Long getPendingOrdersCount() {
        return repo.getPendingOrdersCount();
    }

    // ============================================================
    // Số đơn pending theo date range
    // ============================================================
    public Long getPendingOrdersCountByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return repo.getPendingOrdersCountByDateRange(startDate, endDate);
    }

    // ============================================================
    // Tất cả giao dịch (không giới hạn)
    // ============================================================
    public List<RevenueDTO> getAllTransactions() {
        return mapToDTO(repo.getAllTransactions());
    }

    // ============================================================
    // Giao dịch theo date range
    // ============================================================
    public List<RevenueDTO> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return mapToDTO(repo.getOrdersByDateRange(startDate, endDate));
    }

    // ============================================================
    // Map Object[] -> RevenueDTO
    // Thứ tự: [0] OrderCode, [1] FullName, [2] CreatedAt,
    //         [3] TotalAmount, [4] Method, [5] Status
    // ============================================================
    private List<RevenueDTO> mapToDTO(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String orderCode    = row[0] != null ? row[0].toString() : "";
            String customerName = row[1] != null ? row[1].toString() : "";

            LocalDateTime orderDate = null;
            if (row[2] != null) {
                if (row[2] instanceof java.sql.Timestamp) {
                    orderDate = ((java.sql.Timestamp) row[2]).toLocalDateTime();
                } else if (row[2] instanceof LocalDateTime) {
                    orderDate = (LocalDateTime) row[2];
                }
            }

            BigDecimal totalAmount  = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            String paymentMethod    = row[4] != null ? row[4].toString() : null;
            String status           = row[5] != null ? row[5].toString() : "";

            return new RevenueDTO(orderCode, customerName, orderDate, totalAmount, status, paymentMethod);
        }).collect(Collectors.toList());
    }
}