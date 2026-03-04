package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.dto.HealthCheckContextDTO;
import vn.edu.fpt.petworldplatform.dto.SaveHealthReportDraftRequest;
import vn.edu.fpt.petworldplatform.dto.SubmitHealthReportRequest;
import vn.edu.fpt.petworldplatform.dto.UpdateHealthReportRequest;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HealthCheckService implements IHealthCheckService {

    private static final long REPORT_UPDATE_WINDOW_HOURS = 24;

    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;
    private final PetHealthRecordRepository petHealthRecordRepository;
    private final PetHealthPhotoRepository petHealthPhotoRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public List<Appointment> getAssignedAppointments(Integer staffId) {
        validateStaffActive(staffId);
        return appointmentRepository.findByStaffIdOrderByAppointmentDateDesc(staffId);
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
        validateStaffActive(staffId);
        Appointment appointment = getAppointmentDetail(staffId, appointmentId);

        String status = lower(appointment.getStatus());
        if (!"checked_in".equals(status) && !"in_progress".equals(status)) {
            throw new IllegalStateException("Pet status must be Checked In before Execute.");
        }

        // When staff clicks "Perform SPA Service", move status to in_progress
        if ("checked_in".equals(status)) {
            appointment.setStatus("in_progress");
            appointment.setUpdatedAt(LocalDateTime.now());
            appointment = appointmentRepository.save(appointment);
        }

        return HealthCheckContextDTO.builder()
                .appointmentId(appointment.getId())
                .appointmentCode(appointment.getAppointmentCode())
                .petId(appointment.getPetId())
                .petName(appointment.getPet() != null ? appointment.getPet().getName() : null)
                .status(appointment.getStatus())
                .customerName(appointment.getCustomer() != null ? appointment.getCustomer().getFullName() : null)
                .staffName(appointment.getStaff() != null ? appointment.getStaff().getFullName() : null)
                .build();
    }

    @Override
    @Transactional
    public void submitHealthReport(Integer staffId, Integer appointmentId, SubmitHealthReportRequest request) {
        validateStaffActive(staffId);
        Appointment appointment = getAppointmentDetail(staffId, appointmentId);

        if (!"checked_in".equals(lower(appointment.getStatus())) && !"in_progress".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Invalid status transition. Appointment must be checked_in or in_progress.");
        }

        validateNumericFields(request.getWeightKg(), request.getTemperature());
        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), true);

        PetHealthRecord record = petHealthRecordRepository.findByAppointment_Id(appointmentId)
                .orElseGet(PetHealthRecord::new);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        record.setAppointment(appointment);
        record.setPet(appointment.getPet());
        record.setPerformedByStaff(staff);
        record.setWeightKg(toBigDecimal(request.getWeightKg()));
        record.setTemperature(toBigDecimal(request.getTemperature()));
        record.setConditionBefore(request.getConditionBefore());
        record.setConditionAfter(request.getConditionAfter());
        record.setFindings(request.getFindings());
        record.setRecommendations(request.getRecommendations());
        record.setNote(request.getConditionNotes());
        record.setWarningFlag(Boolean.TRUE.equals(request.getWarningFlag()));
        record.setIsDraft(false);
        record.setIsDeleted(false);
        record.setCheckDate(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        PetHealthRecord saved = petHealthRecordRepository.save(record);

        replacePhotos(saved, storedPhotoUrls);

        appointment.setStatus("done");
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);

        createCustomerNotification(appointment, saved);
    }

    @Override
    @Transactional
    public void saveDraft(Integer staffId, Integer appointmentId, SaveHealthReportDraftRequest request) {
        validateStaffActive(staffId);
        Appointment appointment = getAppointmentDetail(staffId, appointmentId);

        if (!"checked_in".equals(lower(appointment.getStatus())) && !"in_progress".equals(lower(appointment.getStatus()))) {
            throw new IllegalStateException("Draft can only be saved when appointment is checked_in or in_progress.");
        }

        validateNumericFields(request.getWeightKg(), request.getTemperature());
        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), false);

        PetHealthRecord record = petHealthRecordRepository.findByAppointment_Id(appointmentId)
                .orElseGet(PetHealthRecord::new);

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalStateException("Staff not found."));

        record.setAppointment(appointment);
        record.setPet(appointment.getPet());
        record.setPerformedByStaff(staff);
        record.setWeightKg(toBigDecimal(request.getWeightKg()));
        record.setTemperature(toBigDecimal(request.getTemperature()));
        record.setConditionBefore(request.getConditionBefore());
        record.setConditionAfter(request.getConditionAfter());
        record.setFindings(request.getFindings());
        record.setRecommendations(request.getRecommendations());
        record.setNote(request.getConditionNotes());
        record.setWarningFlag(Boolean.TRUE.equals(request.getWarningFlag()));
        record.setIsDraft(true);
        record.setIsDeleted(false);
        record.setUpdatedAt(LocalDateTime.now());

        PetHealthRecord saved = petHealthRecordRepository.save(record);

        if (!storedPhotoUrls.isEmpty()) {
            replacePhotos(saved, storedPhotoUrls);
        }
    }

    @Override
    @Transactional
    public void updateWithin24h(Integer staffId, Integer recordId, UpdateHealthReportRequest request) {
        validateStaffActive(staffId);

        PetHealthRecord record = petHealthRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Health record not found."));

        if (record.getPerformedByStaffId() == null || !record.getPerformedByStaffId().equals(staffId.longValue())) {
            throw new IllegalStateException("You can only update your own health record.");
        }

        validateUpdateWindow(record.getCheckDate());
        validateNumericFields(request.getWeightKg(), request.getTemperature());

        List<String> storedPhotoUrls = storePhotos(request.getPhotos(), false);

        record.setWeightKg(toBigDecimal(request.getWeightKg()));
        record.setTemperature(toBigDecimal(request.getTemperature()));
        record.setConditionBefore(request.getConditionBefore());
        record.setConditionAfter(request.getConditionAfter());
        record.setFindings(request.getFindings());
        record.setRecommendations(request.getRecommendations());
        record.setNote(request.getConditionNotes());
        record.setWarningFlag(Boolean.TRUE.equals(request.getWarningFlag()));
        record.setUpdatedAt(LocalDateTime.now());

        PetHealthRecord saved = petHealthRecordRepository.save(record);

        if (!storedPhotoUrls.isEmpty()) {
            replacePhotos(saved, storedPhotoUrls);
        }
    }

    private Appointment getAppointmentDetail(Integer staffId, Integer appointmentId) {
        return appointmentRepository.findByIdAndAssignedStaff(appointmentId, staffId.longValue())
                .orElseThrow(() -> new IllegalStateException("Appointment not found or not assigned to this staff."));
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

    private void validateUpdateWindow(LocalDateTime checkDate) {
        if (checkDate == null) {
            throw new IllegalStateException("Update period expired");
        }
        long hours = Duration.between(checkDate, LocalDateTime.now()).toHours();
        if (hours > REPORT_UPDATE_WINDOW_HOURS) {
            throw new IllegalStateException("Update period expired");
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

    private void replacePhotos(PetHealthRecord record, List<String> photoUrls) {
        List<PetHealthPhoto> oldItems = petHealthPhotoRepository.findByRecord_Id(record.getId());
        if (!oldItems.isEmpty()) {
            petHealthPhotoRepository.deleteAll(oldItems);
        }

        List<PetHealthPhoto> items = photoUrls.stream()
                .map(url -> PetHealthPhoto.builder()
                        .record(record)
                        .imageUrl(url)
                        .capturedAt(LocalDateTime.now())
                        .build())
                .toList();
        petHealthPhotoRepository.saveAll(items);
    }

    private void createCustomerNotification(Appointment appointment, PetHealthRecord record) {
        if (appointment.getCustomer() == null) {
            return;
        }

        String title = Boolean.TRUE.equals(record.getWarningFlag())
                ? "Health check completed with warning"
                : "Health check completed";

        String message = "Health report for appointment " + appointment.getAppointmentCode()
                + " is available. Status has been updated to done.";

        Notification notification = Notification.builder()
                .customer(appointment.getCustomer())
                .appointment(appointment)
                .title(title)
                .message(message)
                .type("health_report")
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
