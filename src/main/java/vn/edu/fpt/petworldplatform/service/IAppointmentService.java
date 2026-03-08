package vn.edu.fpt.petworldplatform.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface IAppointmentService {
    List<Appointment> getAllAppointments();
    Page<Appointment> getAppointments(AppointmentFilterRequest filter);
    Appointment getAppointmentById(Integer id);
    void assignStaffToServiceLine(Integer appointmentId, Integer lineId, Integer staffId);
    List<AppointmentServiceLine> getServiceLinesByAppointment(Integer appointmentId);
    List<Staff> getAvailableStaffForServiceLine(Integer appointmentId, Integer lineId);
    void cancelAppointment(Integer id, String reason);
    void deleteAppointment(Integer id);
    ByteArrayInputStream exportToExcel(AppointmentFilterRequest filter);
    List<Appointment> getStaffAppointments(Integer staffId, String status);
    void updateStatus(Integer id, String status);
}
