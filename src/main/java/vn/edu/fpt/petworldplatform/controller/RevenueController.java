package vn.edu.fpt.petworldplatform.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.dto.RevenueDTO;
import vn.edu.fpt.petworldplatform.service.RevenueService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/reports")
public class RevenueController {

    private final RevenueService service;

    public RevenueController(RevenueService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    public String revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        // --- Stats cố định (luôn theo hôm nay) ---
        model.addAttribute("todayRevenue",  service.getTodayRevenue());
        model.addAttribute("percentChange", service.getPercentChange());

        String currentMonth = String.format("%02d/%d",
                LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        model.addAttribute("currentMonth", currentMonth);

        // --- Xử lý filter ---
        boolean filtered = (startDate != null || endDate != null);

        BigDecimal filteredRevenue;
        Long pendingOrders;
        List<RevenueDTO> transactions;

        if (filtered) {
            // fallback nếu chỉ chọn 1 trong 2
            LocalDateTime start = (startDate != null)
                    ? startDate.atStartOfDay()
                    : LocalDateTime.of(1900, 1, 1, 0, 0, 0);

            LocalDateTime end = (endDate != null)
                    ? endDate.atTime(23, 59, 59)
                    : LocalDateTime.now();

            filteredRevenue = service.getRevenueByDateRange(start, end);
            pendingOrders   = service.getPendingOrdersCountByDateRange(start, end);
            transactions    = service.getOrdersByDateRange(start, end);
        } else {
            // Không filter → toàn thời gian
            filteredRevenue = service.getAllTimeRevenue();
            pendingOrders   = service.getPendingOrdersCount();
            transactions    = service.getAllTransactions();
        }

        model.addAttribute("filteredRevenue", filteredRevenue);
        model.addAttribute("pendingOrders",   pendingOrders);
        model.addAttribute("transactions",    transactions);
        model.addAttribute("filtered",        filtered);
        model.addAttribute("startDate",       startDate);
        model.addAttribute("endDate",         endDate);

        return "admin/report-revenue";
    }
}