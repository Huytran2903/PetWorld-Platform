package vn.edu.fpt.petworldplatform.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.dto.RevenueDTO;
import vn.edu.fpt.petworldplatform.service.RevenueService;

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

    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    @GetMapping("/revenue")
    public String revenue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            Model model) {

        // --- Stats cards ---
        model.addAttribute("todayRevenue",    service.getTodayRevenue());
        model.addAttribute("percentChange",   service.getPercentChange());
        model.addAttribute("monthlyRevenue",  service.getMonthlyRevenue());
        model.addAttribute("pendingOrders",   service.getPendingOrdersCount());

        // Tháng hiện tại dạng "MM/yyyy" để hiển thị
        String currentMonth = String.format("%02d/%d",
                LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        model.addAttribute("currentMonth", currentMonth);

        // --- Transactions table ---
        List<RevenueDTO> transactions;
        boolean filtered = (startDate != null || endDate != null);

        if (filtered) {
            // Chuyển LocalDate -> LocalDateTime cho query
            LocalDateTime start = startDate != null
                    ? startDate.atStartOfDay()                          // 00:00:00
                    : null;
            LocalDateTime end   = endDate != null
                    ? endDate.atTime(23, 59, 59)                        // 23:59:59
                    : null;
            transactions = service.getOrdersByDateRange(start, end);
        } else {
            transactions = service.getRecentTransactions();
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("filtered",     filtered);
        model.addAttribute("startDate",    startDate);  
        model.addAttribute("endDate",      endDate);

        return "admin/report-revenue";
    }
}