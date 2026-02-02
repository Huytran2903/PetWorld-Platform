package vn.edu.fpt.petworldplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.edu.fpt.petworldplatform.dto.ServiceUsageStatsDTO;
import vn.edu.fpt.petworldplatform.service.ServiceExecutionHistoryService;

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
     * Shows which services are most popular/frequently used
     */
    @GetMapping("/service-stats")
    public String getServiceStats(Model model) {
        try {
            // Get service usage statistics
            List<ServiceUsageStatsDTO> stats = service.getServiceUsageStats();
            model.addAttribute("serviceStats", stats);
            
            // Calculate total bookings
            long totalBookings = stats.stream()
                    .mapToLong(ServiceUsageStatsDTO::getUsageCount)
                    .sum();
            model.addAttribute("totalBookings", totalBookings);
            
        } catch (Exception e) {
            model.addAttribute("error", "Error loading service statistics: " + e.getMessage());
            model.addAttribute("serviceStats", List.of());
            model.addAttribute("totalBookings", 0L);
        }

        return "staff/service-stats";  // Fixed: Added "staff/" prefix
    }
}