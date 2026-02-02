package vn.edu.fpt.petworldplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.dto.ServiceExecutionHistoryDTO;
import vn.edu.fpt.petworldplatform.dto.ServiceUsageStatsDTO;
import vn.edu.fpt.petworldplatform.repository.ServiceExecutionHistoryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServiceExecutionHistoryService {

    private final ServiceExecutionHistoryRepository repo;

    public ServiceExecutionHistoryService(ServiceExecutionHistoryRepository repo) {
        this.repo = repo;
    }

    // ============================================================
    // Lấy số liệu thống kê
    // ============================================================
    public Long getCompletedCount() {
        return repo.getCompletedAppointmentsCount();
    }

    public Long getInProgressCount() {
        return repo.getInProgressAppointmentsCount();
    }

    public Long getPendingCount() {
        return repo.getPendingAppointmentsCount();
    }

    // ============================================================
    // Lấy toàn bộ lịch sử (không filter)
    // ============================================================
    public List<ServiceExecutionHistoryDTO> getAllHistory() {
        List<Object[]> rows = repo.getAllServiceExecutionHistory();
        return mapToDTO(rows);
    }

    // ============================================================
    // Filter theo status
    // ============================================================
    public List<ServiceExecutionHistoryDTO> getHistoryByStatus(String status) {
        List<Object[]> rows = repo.getServiceExecutionHistoryByStatus(status);
        return mapToDTO(rows);
    }

    // ============================================================
    // Filter theo date range
    // ============================================================
    public List<ServiceExecutionHistoryDTO> getHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> rows = repo.getServiceExecutionHistoryByDateRange(startDate, endDate);
        return mapToDTO(rows);
    }

    // ============================================================
    // Filter theo status + date range
    // ============================================================
    public List<ServiceExecutionHistoryDTO> getHistoryByStatusAndDateRange(String status,
                                                                            LocalDateTime startDate,
                                                                            LocalDateTime endDate) {
        List<Object[]> rows = repo.getServiceExecutionHistoryByStatusAndDateRange(status, startDate, endDate);
        return mapToDTO(rows);
    }

    // ============================================================
    // NEW: Lấy thống kê sử dụng dịch vụ (Service Usage Statistics)
    // ============================================================
    public List<ServiceUsageStatsDTO> getServiceUsageStats() {
        List<Object[]> rows = repo.getServiceUsageStatistics();
        return mapToServiceUsageDTO(rows);
    }

    // ============================================================
    // Map Object[] -> ServiceExecutionHistoryDTO
    // [0] AppointmentCode, [1] CustomerName, [2] PetName,
    // [3] ServiceNames (từ STRING_AGG), [4] AppointmentDate, 
    // [5] Status, [6] AssignedStaff (từ STRING_AGG)
    // ============================================================
    private List<ServiceExecutionHistoryDTO> mapToDTO(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String appointmentCode = row[0] != null ? row[0].toString() : "";
            String customerName    = row[1] != null ? row[1].toString() : "";
            String petName         = row[2] != null ? row[2].toString() : "";
            String serviceName     = row[3] != null ? row[3].toString() : "N/A";

            // Handle LocalDateTime or Timestamp
            LocalDateTime appointmentDate = null;
            if (row[4] != null) {
                if (row[4] instanceof java.sql.Timestamp) {
                    appointmentDate = ((java.sql.Timestamp) row[4]).toLocalDateTime();
                } else if (row[4] instanceof LocalDateTime) {
                    appointmentDate = (LocalDateTime) row[4];
                }
            }

            String status        = row[5] != null ? row[5].toString() : "";
            String assignedStaff = row[6] != null ? row[6].toString() : "Unassigned";

            return new ServiceExecutionHistoryDTO(appointmentCode, customerName, petName,
                                                   serviceName, appointmentDate, status, assignedStaff);
        }).collect(Collectors.toList());
    }

    // ============================================================
    // Map Object[] -> ServiceUsageStatsDTO
    // [0] ServiceName, [1] UsageCount, [2] Percentage
    // ============================================================
    private List<ServiceUsageStatsDTO> mapToServiceUsageDTO(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String serviceName = row[0] != null ? row[0].toString() : "Unknown";
            
            Long usageCount = 0L;
            if (row[1] != null) {
                if (row[1] instanceof Integer) {
                    usageCount = ((Integer) row[1]).longValue();
                } else if (row[1] instanceof Long) {
                    usageCount = (Long) row[1];
                }
            }

            BigDecimal percentage = BigDecimal.ZERO;
            if (row[2] != null) {
                if (row[2] instanceof BigDecimal) {
                    percentage = (BigDecimal) row[2];
                } else if (row[2] instanceof Double) {
                    percentage = BigDecimal.valueOf((Double) row[2]);
                }
            }

            return new ServiceUsageStatsDTO(serviceName, usageCount, percentage);
        }).collect(Collectors.toList());
    }
}