package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignedAppointmentService implements IAssignedAppointmentService {

    private static final long CHECK_IN_EARLY_MINUTES = 100000;

    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_CHECKED_IN = "checked_in";

    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;

    @Override
    public List<Appointment> getAssignedAppointments(Integer staffId, String dateFilter, String statusFilter) {
        validateStaffActive(staffId);

        LocalDateTime from = null;
        LocalDateTime to = null;

        if ("today".equalsIgnoreCase(dateFilter)) {
            from = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            to = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        } else if ("tomorrow".equalsIgnoreCase(dateFilter)) {
            from = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN);
            to = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MAX);
        }

        String status = (statusFilter == null || statusFilter.isEmpty()) ? null : statusFilter;

        return appointmentRepository.findByAssignedStaffAndFilter(staffId.longValue(), from, to, status);
    }

    @Override
    public Appointment getAppointmentDetail(Integer staffId, Integer appointmentId) {
        validateStaffActive(staffId);
        return appointmentRepository.findByIdAndAssignedStaff(appointmentId, staffId.longValue())
                .orElseThrow(() -> new RuntimeException("Appointment not found or not assigned to this staff."));
    }

    @Override
    @Transactional
    public Appointment checkIn(Integer staffId, Integer appointmentId) {
        validateStaffActive(staffId);
        Appointment appointment = getAppointmentDetail(staffId, appointmentId);

        String currentStatus = lower(appointment.getStatus());
        if (!STATUS_CONFIRMED.equals(currentStatus)) {
            throw new IllegalStateException("Only confirmed appointments can be checked in.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime earliestCheckIn = appointment.getAppointmentDate().minusMinutes(CHECK_IN_EARLY_MINUTES);
        if (now.isBefore(earliestCheckIn)) {
            throw new IllegalStateException("Too early to Check In");
        }

        return updateStatus(appointment, STATUS_CHECKED_IN);
    }

    @Override
    @Transactional
    public Appointment reportNoShow(Integer staffId, Integer appointmentId) {
        validateStaffActive(staffId);
        Appointment appointment = getAppointmentDetail(staffId, appointmentId);

        String currentStatus = lower(appointment.getStatus());
        if (!(STATUS_CONFIRMED.equals(currentStatus) || STATUS_CHECKED_IN.equals(currentStatus))) {
            throw new IllegalStateException("No-show can only be reported for confirmed or checked-in appointments.");
        }

        appointment.setCanceledAt(LocalDateTime.now());
        appointment.setCancellationReason("No Show");
        return updateStatus(appointment, "no_show");
    }

    private Appointment updateStatus(Appointment appointment, String nextStatus) {
        appointment.setStatus(nextStatus);
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.save(appointment);
    }

    private void validateStaffActive(Integer staffId) {
        if (staffId == null) {
            throw new RuntimeException("Staff not found.");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found."));

        if (staff.getIsActive() == null || !staff.getIsActive()) {
            throw new RuntimeException("Staff account is not active.");
        }
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
