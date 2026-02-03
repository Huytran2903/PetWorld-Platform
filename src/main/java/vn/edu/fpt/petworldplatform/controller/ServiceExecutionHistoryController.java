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

    /**
     * Display Service Execution History page
     * Supports filtering by status and date range
     */
    @GetMapping("/service-execution-history")
    public String getServiceHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {

        try {
            // Get statistics for dashboard cards
            Long completedCount = service.getCompletedCount();
            Long inProgressCount = service.getInProgressCount();
            Long pendingCount = service.getPendingCount();

            model.addAttribute("completedCount", completedCount != null ? completedCount : 0L);
            model.addAttribute("inProgressCount", inProgressCount != null ? inProgressCount : 0L);
            model.addAttribute("pendingCount", pendingCount != null ? pendingCount : 0L);

            // Get history data based on filters
            List<ServiceExecutionHistoryDTO> history;
            boolean filtered = false;

            // Case 1: Filter by both status and date range
            if (isNotEmpty(status) && isNotEmpty(startDate) && isNotEmpty(endDate)) {
                LocalDateTime start = parseDate(startDate, true);
                LocalDateTime end = parseDate(endDate, false);
                history = service.getHistoryByStatusAndDateRange(status, start, end);
                filtered = true;
            }
            // Case 2: Filter by status only
            else if (isNotEmpty(status)) {
                history = service.getHistoryByStatus(status);
                filtered = true;
            }
            // Case 3: Filter by date range only
            else if (isNotEmpty(startDate) && isNotEmpty(endDate)) {
                LocalDateTime start = parseDate(startDate, true);
                LocalDateTime end = parseDate(endDate, false);
                history = service.getHistoryByDateRange(start, end);
                filtered = true;
            }
            // Case 4: No filter - get all
            else {
                history = service.getAllHistory();
            }

            // Add data to model
            model.addAttribute("history", history);
            model.addAttribute("filtered", filtered);
            model.addAttribute("status", status);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

        } catch (Exception e) {
            // If error occurs, return empty list
            model.addAttribute("history", List.of());
            model.addAttribute("filtered", false);
            model.addAttribute("error", "Lỗi khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("completedCount", 0L);
            model.addAttribute("inProgressCount", 0L);
            model.addAttribute("pendingCount", 0L);
            
            // Log error for debugging
            e.printStackTrace();
        }

        return "staff/service-execution-history";
    }

    /**
     * Helper method to check if string is not empty
     */
    private boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Helper method to parse date string to LocalDateTime
     * @param dateStr Date string in format yyyy-MM-dd
     * @param isStartOfDay true for start of day (00:00:00), false for end of day (23:59:59)
     */
    private LocalDateTime parseDate(String dateStr, boolean isStartOfDay) {
        LocalDate date = LocalDate.parse(dateStr);
        return isStartOfDay 
            ? date.atStartOfDay() 
            : date.atTime(LocalTime.MAX);
    }
}