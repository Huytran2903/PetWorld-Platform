package vn.edu.fpt.petworldplatform.service;

import vn.edu.fpt.petworldplatform.dto.HealthCheckContextDTO;
import vn.edu.fpt.petworldplatform.dto.SaveHealthReportDraftRequest;
import vn.edu.fpt.petworldplatform.dto.SubmitHealthReportRequest;
import vn.edu.fpt.petworldplatform.dto.UpdateHealthReportRequest;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.util.List;

public interface IHealthCheckService {
    List<Appointment> getAssignedAppointments(Integer staffId);

    Appointment checkInPet(Integer staffId, Integer appointmentId);

    HealthCheckContextDTO startHealthCheck(Integer staffId, Integer appointmentId);

    void submitHealthReport(Integer staffId, Integer appointmentId, SubmitHealthReportRequest request);

    void saveDraft(Integer staffId, Integer appointmentId, SaveHealthReportDraftRequest request);

    void updateWithin24h(Integer staffId, Integer recordId, UpdateHealthReportRequest request);
}
