package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.WorkShiftDTO;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkScheduleService implements IWorkScheduleService {

    private final StaffRepository staffRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;

    @Override
    public List<WorkShiftDTO> getStaffSchedule(Integer staffId, LocalDate startDate, LocalDate endDate) {
        if (!isStaffActive(staffId)) {
            throw new IllegalStateException("Account is not active");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }

        if (endDate.isBefore(startDate)) {
            LocalDate tmp = startDate;
            startDate = endDate;
            endDate = tmp;
        }

        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.atTime(LocalTime.MAX);
        List<AppointmentServiceLine> assignedLines = appointmentServiceLineRepository
                .findAssignedLinesByStaffAndFilter(staffId, from, to, null);

        Map<Integer, WorkShiftDTO> appointmentBased = new LinkedHashMap<>();
        for (AppointmentServiceLine line : assignedLines) {
            if (line.getAppointment() == null || line.getAppointment().getId() == null) {
                continue;
            }
            if (isExcludedAppointmentStatus(line.getAppointment().getStatus())) {
                continue;
            }
            Integer appointmentId = line.getAppointment().getId();
            if (appointmentBased.containsKey(appointmentId)) {
                continue;
            }

            LocalDateTime apptStart = line.getAppointment().getAppointmentDate();
            LocalDateTime apptEnd = line.getAppointment().getEndTime();
            if (apptStart == null || apptEnd == null) {
                continue;
            }

            String apptCode = line.getAppointment().getAppointmentCode() != null
                    ? line.getAppointment().getAppointmentCode()
                    : "#" + appointmentId;
            String serviceName = (line.getService() != null && line.getService().getName() != null)
                    ? line.getService().getName()
                    : "Assigned service";

            WorkShiftDTO virtualItem = WorkShiftDTO.builder()
                    .workDate(apptStart.toLocalDate())
                    .startTime(apptStart.toLocalTime())
                    .endTime(apptEnd.toLocalTime())
                    .note("From appointment " + apptCode + " - " + serviceName)
                    .displayStatus(line.getAppointment().getStatus())
                    .build();
            appointmentBased.put(appointmentId, virtualItem);
        }

        return appointmentBased.values().stream()
                .sorted((a, b) -> {
                    int d = a.getWorkDate().compareTo(b.getWorkDate());
                    if (d != 0) return d;
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .toList();
    }

    @Override
    public boolean isStaffActive(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));
        return staff.getIsActive() != null && staff.getIsActive();
    }

    /** Không hiển thị lịch suy ra từ appointment đã hủy / từ chối. */
    private static boolean isExcludedAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String s = status.trim().toLowerCase(Locale.ROOT);
        return "canceled".equals(s) || "cancelled".equals(s) || "rejected".equals(s);
    }
}
