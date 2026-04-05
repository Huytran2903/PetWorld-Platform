package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IAssignedAppointmentService {
    List<Appointment> getAssignedAppointments(Integer staffId, String dateFilter, String statusFilter);

    Appointment getAppointmentDetail(Integer staffId, Integer appointmentId);

    Appointment checkIn(Integer staffId, Integer appointmentId);

    Appointment reportNoShow(Integer staffId, Integer appointmentId);

    /**
     * No-show chỉ hợp lệ sau khi khách đã qua hết cửa sổ check-in (hết ân hạn trễ so với giờ hẹn).
     */
    boolean isNoShowReportAllowed(Appointment appointment);

    /**
     * Với lịch {@code confirmed}: mốc giờ sau đó mới được báo no-show (= hết ân hạn check-in trễ).
     */
    Optional<LocalDateTime> getEarliestNoShowReportTime(Appointment appointment);
}
