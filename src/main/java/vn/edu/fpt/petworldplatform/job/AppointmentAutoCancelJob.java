package vn.edu.fpt.petworldplatform.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppointmentAutoCancelJob {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;

    /**
     * Tự động hủy các lịch hẹn đang pending, đã đến giờ hẹn
     * nhưng chưa có bất kỳ service line nào được gán nhân viên.
     *
     * Chạy 5 phút một lần.
     */
    @Scheduled(fixedRate = 300_000)
    public void autoCancelUnassignedAppointments() {
        LocalDateTime now = LocalDateTime.now();

        // Lấy tất cả các lịch pending đã đến giờ hẹn
        List<Appointment> pendingAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() != null && "pending".equalsIgnoreCase(a.getStatus()))
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isAfter(now))
                .toList();

        for (Appointment appointment : pendingAppointments) {
            // Nếu chưa có service line nào được gán nhân viên thì tự hủy
            long assignedCount = appointmentServiceLineRepository
                    .countByAppointment_IdAndAssignedStaffIdIsNotNull(appointment.getId());

            if (assignedCount == 0) {
                appointment.setStatus("canceled");
                appointment.setCancellationReason("Auto-canceled: no staff assigned by appointment time.");
                appointment.setCanceledAt(LocalDateTime.now());
                appointment.setUpdatedAt(LocalDateTime.now());
                appointmentRepository.save(appointment);
            }
        }
    }
}

