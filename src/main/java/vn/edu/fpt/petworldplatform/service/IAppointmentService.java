package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.util.List;

public interface IAppointmentService {
    List<Appointment> getAllAppointments();
    Appointment getAppointmentById(Integer id);
    void assignStaffToAppointment(Integer appointmentId, Long staffId);
    void reassignStaff(Integer appointmentId, Long newStaffId);
}
