package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.dto.AppointmentSummaryRequest;
import vn.edu.fpt.petworldplatform.dto.HealthCheckContextDTO;
import vn.edu.fpt.petworldplatform.dto.SaveHealthReportDraftRequest;
import vn.edu.fpt.petworldplatform.dto.ServiceNoteRequest;
import vn.edu.fpt.petworldplatform.dto.SubmitHealthReportRequest;
import vn.edu.fpt.petworldplatform.dto.UpdateHealthReportRequest;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.util.List;

public interface IHealthCheckService {
    List<Appointment> getAssignedAppointments(Integer staffId);

    Appointment checkInPet(Integer staffId, Integer appointmentId);

    HealthCheckContextDTO startHealthCheck(Integer staffId, Integer appointmentId);

    HealthCheckContextDTO startHealthCheck(Integer staffId, Integer appointmentId, Integer serviceLineId);

    void submitHealthReport(Integer staffId, Integer appointmentId, SubmitHealthReportRequest request);

    void submitHealthReport(Integer staffId, Integer appointmentId, Integer serviceLineId, SubmitHealthReportRequest request);

    void submitServiceNote(Integer staffId, Integer appointmentId, Integer serviceLineId, ServiceNoteRequest request);

    void submitAppointmentSummary(Integer staffId, Integer appointmentId, AppointmentSummaryRequest request);

    void saveDraft(Integer staffId, Integer appointmentId, SaveHealthReportDraftRequest request);

    void saveDraft(Integer staffId, Integer appointmentId, Integer serviceLineId, SaveHealthReportDraftRequest request);

    void updateWithin24h(Integer staffId, Integer recordId, UpdateHealthReportRequest request);
}
