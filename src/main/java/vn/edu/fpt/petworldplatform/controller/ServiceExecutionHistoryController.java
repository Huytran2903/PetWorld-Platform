package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.dto.ServiceExecutionHistoryDTO;
import vn.edu.fpt.petworldplatform.service.ServiceExecutionHistoryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class ServiceExecutionHistoryController {

    private final ServiceExecutionHistoryService service;

    public ServiceExecutionHistoryController(ServiceExecutionHistoryService service) {
        this.service = service;
    }

    @GetMapping("/service-execution-history")
    public String getServiceHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            boolean hasStatus    = isNotEmpty(status);
            boolean hasStartDate = isNotEmpty(startDate);
            boolean hasEndDate   = isNotEmpty(endDate);
            boolean hasDateRange = hasStartDate && hasEndDate;
            boolean filtered     = hasStatus || hasDateRange;

            // Parse date range nếu có
            LocalDateTime start = null;
            LocalDateTime end   = null;
            if (hasDateRange) {
                start = parseDate(startDate, true);
                end   = parseDate(endDate, false);
            }

            // --------------------------------------------------------
            // Stats cards: theo date range nếu có, toàn hệ thống nếu không
            // --------------------------------------------------------
            Long completedCount;
            Long inProgressCount;
            Long pendingCount;

            if (hasDateRange) {
                // Có chọn ngày → filter stats theo ngày
                completedCount   = service.getCompletedCountByDateRange(start, end);
                inProgressCount  = service.getInProgressCountByDateRange(start, end);
                pendingCount     = service.getPendingCountByDateRange(start, end);
            } else {
                // Không chọn ngày → tổng toàn hệ thống
                completedCount   = service.getCompletedCount();
                inProgressCount  = service.getInProgressCount();
                pendingCount     = service.getPendingCount();
            }

            model.addAttribute("completedCount",  completedCount  != null ? completedCount  : 0L);
            model.addAttribute("inProgressCount", inProgressCount != null ? inProgressCount : 0L);
            model.addAttribute("pendingCount",    pendingCount    != null ? pendingCount    : 0L);

            // --------------------------------------------------------
            // History table: filter theo tất cả điều kiện
            // --------------------------------------------------------
            List<ServiceExecutionHistoryDTO> history;

            if (hasStatus && hasDateRange) {
                // Filter cả status lẫn date range
                history = service.getHistoryByStatusAndDateRange(status, start, end);
            } else if (hasStatus) {
                // Chỉ filter theo status
                history = service.getHistoryByStatus(status);
            } else if (hasDateRange) {
                // Chỉ filter theo date range
                history = service.getHistoryByDateRange(start, end);
            } else {
                // Không filter → tất cả
                history = service.getAllHistory();
            }

            model.addAttribute("history",    history);
            model.addAttribute("filtered",   filtered);
            model.addAttribute("status",     status);
            model.addAttribute("startDate",  startDate);
            model.addAttribute("endDate",    endDate);

        } catch (Exception e) {
            model.addAttribute("history",         List.of());
            model.addAttribute("filtered",        false);
            model.addAttribute("error",           "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("completedCount",  0L);
            model.addAttribute("inProgressCount", 0L);
            model.addAttribute("pendingCount",    0L);
            e.printStackTrace();
        }

        return "staff/service-execution-history";
    }

    // ============================================================
    // Helpers
    // ============================================================
    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private LocalDateTime parseDate(String dateStr, boolean isStartOfDay) {
        LocalDate date = LocalDate.parse(dateStr);
        return isStartOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
    }
}