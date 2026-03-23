package vn.edu.fpt.petworldplatform.controller;

import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    @GetMapping("/service-execution-history")
    public String getServiceHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            List<ServiceExecutionHistoryDTO> history;
            boolean filtered = false;

            boolean hasStatus    = isNotEmpty(status);
            boolean hasStartDate = isNotEmpty(startDate);
            boolean hasEndDate   = isNotEmpty(endDate);
            boolean hasDateRange = hasStartDate && hasEndDate;

            LocalDateTime start = hasStartDate ? parseDate(startDate, true)  : null;
            LocalDateTime end   = hasEndDate   ? parseDate(endDate,   false) : null;

            // ── Fetch history ──────────────────────────────────────
            if (hasStatus && hasDateRange) {
                history  = service.getHistoryByStatusAndDateRange(status, start, end);
                filtered = true;
            } else if (hasStatus) {
                history  = service.getHistoryByStatus(status);
                filtered = true;
            } else if (hasDateRange) {
                history  = service.getHistoryByDateRange(start, end);
                filtered = true;
            } else {
                history = service.getAllHistory();
            }

            // ── Fetch stat cards ───────────────────────────────────
            // If a date range is selected → count only within that range
            // If only status is selected  → still count within all time (no date constraint)
            // If nothing selected         → count all time
            Long completedCount;
            Long inProgressCount;
            Long pendingCount;

            if (hasDateRange) {
                // Pass status (may be null) so query can optionally filter by it too
                completedCount   = service.getCompletedCountByDateRange(start, end);
                inProgressCount  = service.getInProgressCountByDateRange(start, end);
                pendingCount     = service.getPendingCountByDateRange(start, end);
            } else {
                completedCount   = service.getCompletedCount();
                inProgressCount  = service.getInProgressCount();
                pendingCount     = service.getPendingCount();
            }

            model.addAttribute("completedCount",   completedCount   != null ? completedCount   : 0L);
            model.addAttribute("inProgressCount",  inProgressCount  != null ? inProgressCount  : 0L);
            model.addAttribute("pendingCount",     pendingCount     != null ? pendingCount     : 0L);

            model.addAttribute("history",   history);
            model.addAttribute("filtered",  filtered);
            model.addAttribute("status",    status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate",   endDate);

        } catch (Exception e) {
            model.addAttribute("history",        List.of());
            model.addAttribute("filtered",       false);
            model.addAttribute("error",          "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("completedCount",  0L);
            model.addAttribute("inProgressCount", 0L);
            model.addAttribute("pendingCount",    0L);
            e.printStackTrace();
        }

        return "staff/service-execution-history";
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private LocalDateTime parseDate(String dateStr, boolean isStartOfDay) {
        LocalDate date = LocalDate.parse(dateStr);
        return isStartOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
    }
}