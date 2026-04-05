package vn.edu.fpt.petworldplatform.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.dto.ServiceUsageStatsDTO;
import vn.edu.fpt.petworldplatform.service.ServiceExecutionHistoryService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/staff")
public class ServiceStatsController {

    private final ServiceExecutionHistoryService service;

    public ServiceStatsController(ServiceExecutionHistoryService service) {
        this.service = service;
    }

    /**
     * Display Service Usage Statistics page
     * - No params  → all-time stats from DB
     * - With fromDate/toDate → stats filtered within that date range
     */
    @PreAuthorize("hasAuthority('VIEW_REPORT')")
    @GetMapping("/service-stats")
    public String getServiceStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,

            Model model) {

        LocalDate today = LocalDate.now();
        String dateError = null;

        try {
            // ── Date Validation ────────────────────────────────────
            if (fromDate != null && fromDate.isAfter(today)) {
                dateError = "From date cannot be in the future.";
            } else if (toDate != null && toDate.isAfter(today)) {
                dateError = "To date cannot be in the future.";
            } else if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                dateError = "From date must be earlier than or equal to To date.";
            }

            List<ServiceUsageStatsDTO> stats;

            boolean isFiltered = (fromDate != null || toDate != null) && dateError == null;

            if (!isFiltered) {
                // No filter → all time
                stats = service.getServiceUsageStats();
            } else {
                // Convert LocalDate to LocalDateTime for DB query
                LocalDateTime startDT = (fromDate != null)
                        ? fromDate.atStartOfDay()
                        : LocalDateTime.of(1900, 1, 1, 0, 0, 0);

                LocalDateTime endDT = (toDate != null)
                        ? toDate.atTime(23, 59, 59)
                        : LocalDateTime.of(2099, 12, 31, 23, 59, 59);

                stats = service.getServiceUsageStatsByDateRange(startDT, endDT);
            }

            long totalBookings = stats.stream()
                    .mapToLong(ServiceUsageStatsDTO::getUsageCount)
                    .sum();

            model.addAttribute("serviceStats", stats);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("filtered", isFiltered);

        } catch (Exception e) {
            model.addAttribute("error", "Error loading service statistics: " + e.getMessage());
            model.addAttribute("serviceStats", List.of());
            model.addAttribute("totalBookings", 0L);
            model.addAttribute("filtered", false);
        }

        // Pass back so inputs retain values after submit
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("maxDate", today);
        model.addAttribute("dateError", dateError);

        model.addAttribute("activePage", "service-stats");
        model.addAttribute("pageTitle", "Service Usage Statistics - PetShop");
        return "staff/service-stats";
    }
}