package vn.edu.fpt.petworldplatform.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface IAppointmentService {
    List<Appointment> getAllAppointments();
    Page<Appointment> getAppointments(AppointmentFilterRequest filter);
    Appointment getAppointmentById(Integer id);
    void assignStaffToAppointment(Integer appointmentId, Integer staffId);
    void reassignStaff(Integer appointmentId, Integer newStaffId);
    void cancelAppointment(Integer id, String reason);
    void deleteAppointment(Integer id);
    ByteArrayInputStream exportToExcel(AppointmentFilterRequest filter);
    List<Staff> getAvailableStaffForAppointment(Integer appointmentId);
    List<Appointment> getStaffAppointments(Integer staffId, String status);
    void updateStatus(Integer id, String status);
}
