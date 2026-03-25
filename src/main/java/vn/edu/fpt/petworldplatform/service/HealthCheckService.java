package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.dto.AppointmentSummaryRequest;
import vn.edu.fpt.petworldplatform.dto.HealthCheckContextDTO;
import vn.edu.fpt.petworldplatform.dto.SaveHealthReportDraftRequest;
import vn.edu.fpt.petworldplatform.dto.ServiceNoteRequest;
import vn.edu.fpt.petworldplatform.dto.SubmitHealthReportRequest;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthCheckService implements IHealthCheckService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;
    private final StaffRepository staffRepository;
    private final ServiceNoteRepository serviceNoteRepository;
    private final ServiceNotePhotoRepository serviceNotePhotoRepository;
    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final AppointmentSummaryPhotoRepository appointmentSummaryPhotoRepository;
    private final PetVaccinationRepository petVaccinationRepository;
    private final NotificationService notificationService;

    @Override
    public List<Appointment> getAssignedAppointments(Integer staffId) {
        validateStaffActive(staffId);
        List<Appointment> list = new ArrayList<>();
        for (Appointment a : appointmentRepository.findByStaffIdOrderByAppointmentDateDesc(staffId)) {
            if (!isExcludedFromStaffWorklist(a.getStatus())) {
                list.add(a);
            }
        }
        list.sort(Comparator
                .comparing((Appointment ap) -> isDoneAppointmentStatus(ap.getStatus()))
                .thenComparing(Appointment::getAppointmentDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return list;
    }

    private static boolean isDoneAppointmentStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return "done".equals(status.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isExcludedFromStaffWorklist(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String s = status.trim().toLowerCase(Locale.ROOT);
        return "canceled".equals(s) || "cancelled".equals(s) || "rejected".equals(s);
    }

    @Override
    @Transactional
    public Appointment checkInPet(Integer staffId, Integer appointmentId) {
        validateStaffActive(staffId);

        Appointment appointment = getAppointmentDetail(staffId, appointmentId);
        if (!"confirmed".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Only confirmed appointments can be checked in.");
        }

        appointment.setStatus("checked_in");
        appointment.setUpdatedAt(LocalDateTime.now());
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public HealthCheckContextDTO startHealthCheck(Integer staffId, Integer appointmentId) {
        getAppointmentDetail(staffId, appointmentId);

        Integer serviceLineId = appointmentServiceLineRepository
                .findByAppointment_IdAndAssignedStaffId(appointmentId, staffId)
                .stream()
                .map(AppointmentServiceLine::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No assigned service line found for this staff."));

        return startHealthCheck(staffId, appointmentId, serviceLineId);
    }

    @Override
    @Transactional
    public HealthCheckContextDTO startHealthCheck(Integer staffId, Integer appointmentId, Integer serviceLineId) {
        validateStaffActive(staffId);

        Appointment appointment = getAppointmentDetail(staffId, appointmentId);
        AppointmentServiceLine serviceLine = getServiceLineDetail(staffId, appointmentId, serviceLineId);

        String status = lower(appointment.getStatus());
        if (!"checked_in".equals(status) && !"in_progress".equals(status)) {
            throw new IllegalStateException("Pet status must be Checked In before Execute.");
        }

        if ("checked_in".equals(status)) {
            appointment.setStatus("in_progress");
            appointment.setUpdatedAt(LocalDateTime.now());
            appointment = appointmentRepository.save(appointment);
        }

        markServiceLineInProgress(serviceLine);

        boolean isManager = appointment.getStaffId() != null
                && appointment.getStaffId().equals(staffId);

        boolean oneTimeVaccine = serviceLine.getService() != null
                && Boolean.TRUE.equals(serviceLine.getService().getIsOneTimeVaccine());

        return HealthCheckContextDTO.builder()
                .appointmentId(appointment.getId())
                .serviceLineId(serviceLine.getId())
                .serviceName(serviceLine.getService() != null ? serviceLine.getService().getName() : null)
                .serviceType(serviceLine.getService() != null ? serviceLine.getService().getServiceType() : null)
                .oneTimeVaccine(oneTimeVaccine)
                .appointmentCode(appointment.getAppointmentCode())
                .petId(appointment.getPetId())
                .petName(appointment.getPet() != null ? appointment.getPet().getName() : null)
                .status(appointment.getStatus())
                .customerName(appointment.getCustomer() != null ? appointment.getCustomer().getFullName() : null)
                .staffName(appointment.getStaff() != null ? appointment.getStaff().getFullName() : null)
                .manager(isManager)
                .build();
    }

    @Override
    @Transactional
    public void submitHealthReport(Integer staffId, Integer appointmentId, SubmitHealthReportRequest request) {
        Integer serviceLineId = appointmentServiceLineRepository
                .findByAppointment_IdAndAssignedStaffId(appointmentId, staffId)
                .stream()
                .map(AppointmentServiceLine::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No assigned service line found for this staff."));

        submitHealthReport(staffId, appointmentId, serviceLineId, request);
    }

    @Override
    @Transactional
    public void submitHealthReport(Integer staffId, Integer appointmentId, Integer serviceLineId, SubmitHealthReportRequest request) {
        validateStaffActive(staffId);

        Appointment appointment = getAppointmentDetail(staffId, appointmentId);
        AppointmentServiceLine serviceLine = getServiceLineDetail(staffId, appointmentId, serviceLineId);

        if (!"checked_in".equals(lower(appointment.getStatus())) && !"in_progress".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Invalid status transition. Appointment must be checked_in or in_progress.");
        }

        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), false);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        ServiceNote note = serviceNoteRepository
                .findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(appointmentId, serviceLineId, staffId)
                .orElseGet(ServiceNote::new);

        note.setAppointment(appointment);
        note.setServiceLine(serviceLine);
        note.setStaff(staff);
        note.setNote(request.getConditionNotes());
        note.setStatus("done");

        ServiceNote savedNote = serviceNoteRepository.save(note);
        serviceNotePhotoRepository.deleteByServiceNote_Id(savedNote.getId());
        replaceServiceNotePhotos(savedNote, storedPhotoUrls);

        markServiceLineDone(serviceLine);
        refreshAppointmentStatusByServiceLines(appointment);

        // Appointment summary is handled by manager after all service lines are done

        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void saveDraft(Integer staffId, Integer appointmentId, SaveHealthReportDraftRequest request) {
        Integer serviceLineId = appointmentServiceLineRepository
                .findByAppointment_IdAndAssignedStaffId(appointmentId, staffId)
                .stream()
                .map(AppointmentServiceLine::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No assigned service line found for this staff."));

        saveDraft(staffId, appointmentId, serviceLineId, request);
    }

    @Override
    @Transactional
    public void saveDraft(Integer staffId, Integer appointmentId, Integer serviceLineId, SaveHealthReportDraftRequest request) {
        validateStaffActive(staffId);

        Appointment appointment = getAppointmentDetail(staffId, appointmentId);
        AppointmentServiceLine serviceLine = getServiceLineDetail(staffId, appointmentId, serviceLineId);

        if (!"checked_in".equals(lower(appointment.getStatus())) && !"in_progress".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Draft can only be saved when appointment is checked_in or in_progress.");
        }

        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), false);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        ServiceNote note = serviceNoteRepository
                .findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(appointmentId, serviceLineId, staffId)
                .orElseGet(ServiceNote::new);

        note.setAppointment(appointment);
        note.setServiceLine(serviceLine);
        note.setStaff(staff);
        note.setNote(request.getConditionNotes());
        note.setStatus("draft");

        ServiceNote savedNote = serviceNoteRepository.save(note);
        serviceNotePhotoRepository.deleteByServiceNote_Id(savedNote.getId());
        replaceServiceNotePhotos(savedNote, storedPhotoUrls);

        markServiceLineInProgress(serviceLine);
        refreshAppointmentStatusByServiceLines(appointment);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void submitServiceNote(Integer staffId, Integer appointmentId, Integer serviceLineId, ServiceNoteRequest request) {
        validateStaffActive(staffId);

        Appointment appointment = getAppointmentDetail(staffId, appointmentId);
        AppointmentServiceLine serviceLine = getServiceLineDetail(staffId, appointmentId, serviceLineId);

        if (!"checked_in".equals(lower(appointment.getStatus())) && !"in_progress".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Appointment must be checked_in or in_progress.");
        }

        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), false);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        ServiceNote note = serviceNoteRepository
                .findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(appointmentId, serviceLineId, staffId)
                .orElseGet(ServiceNote::new);

        note.setAppointment(appointment);
        note.setServiceLine(serviceLine);
        note.setStaff(staff);
        note.setNote(request.getNote());
        note.setStatus("done");

        ServiceNote savedNote = serviceNoteRepository.save(note);
        serviceNotePhotoRepository.deleteByServiceNote_Id(savedNote.getId());
        replaceServiceNotePhotos(savedNote, storedPhotoUrls);

        markServiceLineDone(serviceLine);
        createVaccinationRecordIfNeeded(staff, appointment, serviceLine, request);
        refreshAppointmentStatusByServiceLines(appointment);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    private void createVaccinationRecordIfNeeded(Staff staff, Appointment appointment, AppointmentServiceLine serviceLine, ServiceNoteRequest request) {
        if (staff == null || appointment == null || serviceLine == null) return;
        if (serviceLine.getService() == null) return;

        String type = serviceLine.getService().getServiceType();
        if (type == null) return;
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        if (!"vaccine".equals(normalized) && !"vaccination".equals(normalized)) return;

        if (appointment.getPet() == null || appointment.getPet().getId() == null) {
            throw new IllegalStateException("Pet not found for vaccination record.");
        }

        // Vaccine name is locked to the service name to avoid mismatches.
        String vaccineName = (serviceLine.getService().getName() != null ? serviceLine.getService().getName().trim() : "");
        if (vaccineName.isEmpty()) {
            throw new IllegalStateException("Vaccine name is required.");
        }

        LocalDate today = LocalDate.now();
        petVaccinationRepository
                .findTopByPet_PetIDAndVaccineNameIgnoreCaseOrderByAdministeredDateDescCreatedAtDesc(appointment.getPet().getId(), vaccineName)
                .ifPresent(latest -> {
                    LocalDate nextDue = latest.getNextDueDate();
                    if (nextDue == null) {
                        throw new IllegalStateException("This pet already has vaccine '" + vaccineName + "' with missing next due date. Cannot record again.");
                    }
                    if (today.isBefore(nextDue)) {
                        throw new IllegalStateException("Vaccine '" + vaccineName + "' is not eligible until " + nextDue + ".");
                    }
                });

        // Vaccines with recurring schedule must have a Next Due Date.
        boolean oneTimeVaccine = Boolean.TRUE.equals(serviceLine.getService().getIsOneTimeVaccine());
        if (!oneTimeVaccine && (request == null || request.getNextDueDate() == null)) {
            throw new IllegalArgumentException("Next Due Date is required for recurring vaccines.");
        }

        if (!oneTimeVaccine && request.getNextDueDate() != null && !request.getNextDueDate().isAfter(today)) {
            throw new IllegalArgumentException("Next Due Date must be after the administered date.");
        }

        PetVaccinations v = new PetVaccinations();
        v.setPet(appointment.getPet());
        v.setAppointment(appointment);
        v.setPerformedByStaff(staff);
        v.setVaccineName(vaccineName);
        v.setAdministeredDate(today);
        if (request != null) {
            v.setNextDueDate(request.getNextDueDate());
            v.setNote(request.getVaccineNote());
        }
        petVaccinationRepository.save(v);
    }

    @Override
    @Transactional
    public void submitAppointmentSummary(Integer staffId, Integer appointmentId, AppointmentSummaryRequest request) {
        validateStaffActive(staffId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalStateException("Appointment not found."));

        String oldStatus = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : null;

        if (!isAppointmentManager(appointment, staffId)) {
            throw new IllegalStateException("Only appointment manager can submit summary.");
        }

        // Ensure all service lines are done before allowing summary, regardless of appointment status label
        List<AppointmentServiceLine> allLines = appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
        boolean allDone = !allLines.isEmpty() && allLines.stream()
                .allMatch(line -> "done".equals(lower(line.getServiceStatus())));
        if (!allDone) {
            throw new IllegalStateException("All service lines must be done before summary.");
        }

        if (request == null) {
            throw new IllegalArgumentException("Health summary is required.");
        }

        // Prevent "submit empty summary" => accidentally marking appointment as done.
        if (request.getWeightKg() == null) {
            throw new IllegalArgumentException("Weight is required.");
        }
        if (request.getTemperature() == null) {
            throw new IllegalArgumentException("Temperature is required.");
        }
        if (isBlank(request.getConditionBefore())) {
            throw new IllegalArgumentException("Condition Before is required.");
        }
        if (isBlank(request.getConditionAfter())) {
            throw new IllegalArgumentException("Condition After is required.");
        }
        if (isBlank(request.getFindings())) {
            throw new IllegalArgumentException("Findings is required.");
        }
        if (isBlank(request.getRecommendations())) {
            throw new IllegalArgumentException("Recommendations is required.");
        }

        validateNumericFields(request.getWeightKg(), request.getTemperature());

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        AppointmentSummary summary = appointmentSummaryRepository.findByAppointment_Id(appointmentId)
                .orElseGet(AppointmentSummary::new);

        // Store any newly uploaded photos for the summary
        List<String> newPhotoUrls = storePhotos(request.getPhotos(), false);

        List<String> allPhotoUrls = new ArrayList<>();
        if (request.getSelectedExistingPhotos() != null) {
            allPhotoUrls.addAll(request.getSelectedExistingPhotos());
        }
        if (newPhotoUrls != null) {
            allPhotoUrls.addAll(newPhotoUrls);
        }

        summary.setAppointment(appointment);
        summary.setWeightKg(toBigDecimal(request.getWeightKg()));
        summary.setTemperature(toBigDecimal(request.getTemperature()));
        summary.setConditionBefore(request.getConditionBefore());
        summary.setConditionAfter(request.getConditionAfter());
        summary.setFindings(request.getFindings());
        summary.setRecommendations(request.getRecommendations());
        summary.setNote(request.getNote());
        summary.setWarningFlag(Boolean.TRUE.equals(request.getWarningFlag()));
        summary.setSummaryByStaff(staff);

        AppointmentSummary savedSummary = appointmentSummaryRepository.save(summary);

        // Replace summary evidence photos with chosen ones
        appointmentSummaryPhotoRepository.deleteBySummary_Id(savedSummary.getId());
        if (allPhotoUrls != null && !allPhotoUrls.isEmpty()) {
            replaceSummaryPhotos(savedSummary, allPhotoUrls);
        }

        // After manager submits summary, mark appointment as done
        appointment.setStatus("done");
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        boolean transitioningToDone = oldStatus == null || !"done".equalsIgnoreCase(oldStatus);
        if (transitioningToDone) {
            String title = "Service completed";
            String apptCode = appointment.getAppointmentCode() != null ? appointment.getAppointmentCode() : "-";
            String apptDate = appointment.getAppointmentDate() != null
                    ? appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "-";

            String message = "Service for appointment " + apptCode + " (" + apptDate + ") is complete. "
                    + "Please pay to finalize your booking.";

            notificationService.createForCustomer(
                    appointment.getCustomer(),
                    appointment,
                    title,
                    message,
                    "appointment_done"
            );
        }
    }

    private Appointment getAppointmentDetail(Integer staffId, Integer appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalStateException("Appointment not found or not assigned to this staff."));

        boolean isAssigned = !appointmentServiceLineRepository
                .findByAppointment_IdAndAssignedStaffId(appointmentId, staffId)
                .isEmpty();

        if (!isAssigned) {
            throw new IllegalStateException("Appointment not found or not assigned to this staff.");
        }

        return appointment;
    }

    private AppointmentServiceLine getServiceLineDetail(Integer staffId, Integer appointmentId, Integer serviceLineId) {
        AppointmentServiceLine line = appointmentServiceLineRepository.findById(serviceLineId)
                .orElseThrow(() -> new IllegalStateException("Service line not found."));

        if (line.getAppointment() == null || !line.getAppointment().getId().equals(appointmentId)) {
            throw new IllegalStateException("Service line does not belong to appointment.");
        }

        if (line.getAssignedStaffId() == null || !line.getAssignedStaffId().equals(staffId)) {
            throw new IllegalStateException("Service line not assigned to this staff.");
        }

        return line;
    }

    private void markServiceLineInProgress(AppointmentServiceLine line) {
        String status = lower(line.getServiceStatus());
        if ("assigned".equals(status) || "pending".equals(status)) {
            line.setServiceStatus("in_progress");
            appointmentServiceLineRepository.save(line);
        }
    }

    private void markServiceLineDone(AppointmentServiceLine line) {
        line.setServiceStatus("done");
        appointmentServiceLineRepository.save(line);
    }

    private void refreshAppointmentStatusByServiceLines(Appointment appointment) {
        List<AppointmentServiceLine> allLines = appointmentServiceLineRepository
                .findByAppointment_Id(appointment.getId());

        if (allLines.isEmpty()) {
            return;
        }

        boolean allDone = allLines.stream().allMatch(l -> "done".equals(lower(l.getServiceStatus())));
        if (allDone) {
            // All services are done but manager might not have submitted summary yet.
            // Keep appointment in_progress until summary is created; submitAppointmentSummary will set it to done.
            appointment.setStatus("in_progress");
            appointment.setUpdatedAt(LocalDateTime.now());
            return;
        }

        boolean anyInProgress = allLines.stream().anyMatch(l -> "in_progress".equals(lower(l.getServiceStatus())));
        if (anyInProgress) {
            appointment.setStatus("in_progress");
            appointment.setUpdatedAt(LocalDateTime.now());
        }
    }

    private void validateStaffActive(Integer staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        if (staff.getIsActive() == null || !staff.getIsActive()) {
            throw new IllegalStateException("Staff account is not active.");
        }
    }

    private void validateNumericFields(Double weight, Double temperature) {
        if (weight != null && (weight <= 0 || weight > 300)) {
            throw new IllegalStateException("Weight must be a valid positive number.");
        }

        if (temperature != null && (temperature < 30 || temperature > 45)) {
            throw new IllegalStateException("Temperature must be a valid number in range.");
        }
    }

    private List<String> storePhotos(List<MultipartFile> photos, boolean required) {
        List<String> urls = new ArrayList<>();

        if (photos == null) {
            if (required) {
                throw new IllegalStateException("Evidence image is required");
            }
            return urls;
        }

        String projectDir = System.getProperty("user.dir");
        Path srcPath = Paths.get(projectDir, "src", "main", "resources", "static", "images");
        Path targetPath = Paths.get(projectDir, "target", "classes", "static", "images");

        try {
            if (!Files.exists(srcPath)) {
                Files.createDirectories(srcPath);
            }

            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath);
            }

            for (MultipartFile photo : photos) {
                if (photo == null || photo.isEmpty()) {
                    continue;
                }

                String originalName = photo.getOriginalFilename() == null ? "photo.jpg" : photo.getOriginalFilename();
                String fileName = "health-" + UUID.randomUUID() + "-" + originalName;

                Path srcFile = srcPath.resolve(fileName);
                try (InputStream inputStream = photo.getInputStream()) {
                    Files.copy(inputStream, srcFile, StandardCopyOption.REPLACE_EXISTING);
                }

                Path targetFile = targetPath.resolve(fileName);
                Files.copy(srcFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

                urls.add("/images/" + fileName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot store evidence images: " + e.getMessage(), e);
        }

        if (required && urls.isEmpty()) {
            throw new IllegalStateException("Evidence image is required");
        }

        return urls;
    }

    private void replaceServiceNotePhotos(ServiceNote note, List<String> photoUrls) {
        if (note == null || photoUrls == null || photoUrls.isEmpty()) {
            return;
        }

        List<ServiceNotePhoto> items = photoUrls.stream()
                .map(url -> ServiceNotePhoto.builder()
                        .serviceNote(note)
                        .imageUrl(url)
                        .build())
                .toList();

        serviceNotePhotoRepository.saveAll(items);
    }

    private void replaceSummaryPhotos(AppointmentSummary summary, List<String> photoUrls) {
        if (summary == null || photoUrls == null || photoUrls.isEmpty()) {
            return;
        }

        List<AppointmentSummaryPhoto> items = photoUrls.stream()
                .map(url -> AppointmentSummaryPhoto.builder()
                        .summary(summary)
                        .imageUrl(url)
                        .build())
                .toList();

        appointmentSummaryPhotoRepository.saveAll(items);
    }

    private boolean isAppointmentManager(Appointment appointment, Integer staffId) {
        return appointment != null
                && appointment.getStaffId() != null
                && appointment.getStaffId().equals(staffId);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
