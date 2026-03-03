package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.util.List;

public interface IAssignedAppointmentService {
    List<Appointment> getAssignedAppointments(Integer staffId, String dateFilter, String statusFilter);

    Appointment getAppointmentDetail(Integer staffId, Integer appointmentId);

    Appointment checkIn(Integer staffId, Integer appointmentId);

    Appointment reportNoShow(Integer staffId, Integer appointmentId);
}
