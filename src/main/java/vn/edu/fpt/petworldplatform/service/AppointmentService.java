package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;

import vn.edu.fpt.petworldplatform.entity.Appointment;

import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;

import vn.edu.fpt.petworldplatform.entity.Staff;

import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;

import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;

import vn.edu.fpt.petworldplatform.repository.StaffRepository;
import vn.edu.fpt.petworldplatform.repository.NotificationRepository;

import vn.edu.fpt.petworldplatform.repository.spec.AppointmentSpecifications;

import java.io.ByteArrayInputStream;

import java.io.ByteArrayOutputStream;

import java.io.IOException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;

import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor

public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;

    private final AppointmentServiceLineRepository appointmentServiceLineRepository;

    private final StaffRepository staffRepository;

    private final NotificationRepository notificationRepository;

    private final NotificationService notificationService;

    private final EmailService emailService;

    @Override

    public List<Appointment> getAllAppointments() {

        return appointmentRepository.findAll(Sort.by(Sort.Direction.DESC, "appointmentDate"));

    }

    @Override

    public Page<Appointment> getAppointments(AppointmentFilterRequest filter) {

        Specification<Appointment> spec = Specification.where(AppointmentSpecifications.hasStatus(filter.getStatus()))

                .and(AppointmentSpecifications.appointmentDateFrom(filter.getFromDate()))

                .and(AppointmentSpecifications.appointmentDateTo(filter.getToDate()))

                .and(AppointmentSpecifications.keywordLike(filter.getKeyword()));

        return appointmentRepository.findAll(spec,
                PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(Sort.Direction.DESC, "appointmentDate")));

    }

    @Override

    public void cancelAppointment(Integer id, String reason) {

        Appointment appointment = getAppointmentById(id);

        String status = appointment.getStatus().toLowerCase();

        if ("done".equals(status) || "canceled".equals(status) || "no_show".equals(status)) {

            throw new IllegalStateException("Cannot cancel an appointment that is already " + status);

        }

        appointment.setStatus("canceled");

        appointment.setCancellationReason(reason);

        appointment.setCanceledAt(LocalDateTime.now());

        appointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(appointment);

    }

    @Override
    @Transactional
    public void deleteAppointment(Integer id) {

        Appointment appointment = getAppointmentById(id);

        String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "";

        if (!"canceled".equals(status)) {

            throw new IllegalStateException("Only canceled appointments can be deleted.");

        }

        notificationRepository.deleteByAppointment_Id(id);
        appointmentRepository.delete(appointment);

    }

    @Override

    public ByteArrayInputStream exportToExcel(AppointmentFilterRequest filter) {

        filter.setSize(Integer.MAX_VALUE);

        List<Appointment> appointments = getAppointments(filter).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Appointments");

            Row headerRow = sheet.createRow(0);

            String[] columns = { "Code", "Customer ID", "Pet ID", "Date", "Status", "Note", "Cancel Reason" };

            for (int i = 0; i < columns.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(columns[i]);

                CellStyle headerStyle = workbook.createCellStyle();

                Font font = workbook.createFont();

                font.setBold(true);

                headerStyle.setFont(font);

                cell.setCellStyle(headerStyle);

            }

            int rowIdx = 1;

            for (Appointment appt : appointments) {

                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(appt.getAppointmentCode());

                row.createCell(1).setCellValue(appt.getCustomerId());

                row.createCell(2).setCellValue(appt.getPetId());

                row.createCell(3).setCellValue(appt.getAppointmentDate().toString());

                row.createCell(4).setCellValue(appt.getStatus());

                row.createCell(5).setCellValue(appt.getNote());

                row.createCell(6).setCellValue(appt.getCancellationReason());

            }

            workbook.write(out);

            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {

            throw new RuntimeException("Failed to export data to Excel", e);

        }

    }

    @Override

    public Appointment getAppointmentById(Integer id) {

        return appointmentRepository.findById(id)

                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));

    }

    @Override

    @Transactional

    public void assignStaffToServiceLine(Integer appointmentId, Integer lineId, Integer staffId) {

        Appointment appointment = getAppointmentById(appointmentId);

        String oldStatus = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : null;

        if (!("pending".equalsIgnoreCase(appointment.getStatus())

                || "confirmed".equalsIgnoreCase(appointment.getStatus())

                || "checked_in".equalsIgnoreCase(appointment.getStatus()))) {

            throw new IllegalStateException("Only pending/confirmed/checked_in appointments can be assigned.");

        }

        AppointmentServiceLine line = appointmentServiceLineRepository.findById(lineId)

                .orElseThrow(() -> new IllegalArgumentException("Service line not found: " + lineId));

        if (!line.getAppointment().getId().equals(appointmentId)) {

            throw new IllegalArgumentException("Service line does not belong to this appointment.");

        }

        Staff staff = staffRepository.findById(staffId)

                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (!Boolean.TRUE.equals(staff.getIsActive())) {

            throw new IllegalStateException("Staff is not active.");

        }

        long conflictCount = appointmentServiceLineRepository.countOverlappingAssignedLines(

                staffId,

                appointmentId,

                appointment.getAppointmentDate(),

                appointment.getEndTime()

        );

        if (conflictCount > 0) {

            throw new IllegalStateException("Staff is busy in this time slot.");

        }

        line.setAssignedStaff(staff);
        if (line.getServiceStatus() == null || "pending".equalsIgnoreCase(line.getServiceStatus())) {
            line.setServiceStatus("assigned");
        }
        appointmentServiceLineRepository.save(line);

        emailService.sendServiceAssignmentEmail(staff, appointment, line);

        if (appointment.getStaffId() == null) {
            appointment.setStaff(staff);
        }

        long assignedCount = appointmentServiceLineRepository
                .countByAppointment_IdAndAssignedStaffIdIsNotNull(appointmentId);

        long totalCount = appointmentServiceLineRepository.countByAppointment_Id(appointmentId);

        if (assignedCount == totalCount && totalCount > 0) {

            boolean transitioningToConfirmed = oldStatus == null || !"confirmed".equalsIgnoreCase(oldStatus);

            appointment.setStatus("confirmed");

            appointment.setUpdatedAt(LocalDateTime.now());

            appointmentRepository.save(appointment);

            if (transitioningToConfirmed) {
                String title = "Appointment confirmed";
                String apptCode = appointment.getAppointmentCode() != null ? appointment.getAppointmentCode() : "-";
                String apptDate = appointment.getAppointmentDate() != null
                        ? appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-";
                String message = "Your appointment " + apptCode + " has been confirmed for " + apptDate + ".";

                notificationService.createForCustomer(
                        appointment.getCustomer(),
                        appointment,
                        title,
                        message,
                        "appointment_confirmed"
                );
            }

        } else if ("pending".equalsIgnoreCase(appointment.getStatus())) {

            appointment.setUpdatedAt(LocalDateTime.now());

            appointmentRepository.save(appointment);

        }

    }

    @Override

    public List<AppointmentServiceLine> getServiceLinesByAppointment(Integer appointmentId) {

        getAppointmentById(appointmentId);

        return appointmentServiceLineRepository.findByAppointment_Id(appointmentId);

    }

    @Override

    public List<Staff> getAvailableStaffForServiceLine(Integer appointmentId, Integer lineId) {

        Appointment appointment = getAppointmentById(appointmentId);

        AppointmentServiceLine line = appointmentServiceLineRepository.findById(lineId)

                .orElseThrow(() -> new IllegalArgumentException("Service line not found: " + lineId));

        if (!line.getAppointment().getId().equals(appointmentId)) {

            throw new IllegalArgumentException("Service line does not belong to this appointment.");

        }

        List<Staff> activeStaff = staffRepository.findByIsActiveTrue();

        final Integer assignedStaffIdOnLine = line.getAssignedStaffId();

        return activeStaff.stream().filter(staff -> {

            if (isAdminRole(staff)
                    && (assignedStaffIdOnLine == null || !assignedStaffIdOnLine.equals(staff.getStaffId()))) {
                return false;
            }

            long conflictCount = appointmentServiceLineRepository.countOverlappingAssignedLines(

                    staff.getStaffId(),

                    appointmentId,

                    appointment.getAppointmentDate(),

                    appointment.getEndTime()

            );

            return conflictCount == 0;

        }).collect(Collectors.toList());

    }

    @Override

    public List<Appointment> getStaffAppointments(Integer staffId, String status) {

        if (status != null && !status.isEmpty()) {

            return appointmentRepository.findByStaffIdAndStatusOrderByAppointmentDateDesc(staffId, status);

        }

        return appointmentRepository.findByStaffIdOrderByAppointmentDateDesc(staffId);

    }

    @Override
    public void updateStatus(Integer id, String status) {
        Appointment appointment = getAppointmentById(id);

        List<String> validStatuses = List.of("pending", "confirmed", "in_progress", "done", "canceled", "no_show");
        if (!validStatuses.contains(status.toLowerCase())) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        appointment.setStatus(status.toLowerCase());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void updateAppointmentManager(Integer appointmentId, Integer staffId) {
        Appointment appointment = getAppointmentById(appointmentId);

        String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "";
        if ("canceled".equals(status) || "no_show".equals(status)) {
            throw new IllegalStateException("Cannot change manager for canceled or no_show appointments.");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (!Boolean.TRUE.equals(staff.getIsActive())) {
            throw new IllegalStateException("Selected staff is not active.");
        }

        // Ensure the selected staff is assigned to at least one service line of this appointment
        List<AppointmentServiceLine> assignedLines =
                appointmentServiceLineRepository.findByAppointment_IdAndAssignedStaffId(appointmentId, staffId);
        if (assignedLines.isEmpty()) {
            throw new IllegalStateException("Selected staff is not assigned to this appointment.");
        }

        appointment.setStaff(staff);
        appointment.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    /** Admins are not offered for new service-line assignment (unless already assigned on that line). */
    private static boolean isAdminRole(Staff staff) {
        if (staff == null || staff.getRole() == null || staff.getRole().getRoleName() == null) {
            return false;
        }
        return "admin".equalsIgnoreCase(staff.getRole().getRoleName().trim());
    }
}
