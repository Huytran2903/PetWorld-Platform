package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.StaffRepository;
import vn.edu.fpt.petworldplatform.repository.spec.AppointmentSpecifications;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final StaffRepository staffRepository;

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

        return appointmentRepository.findAll(spec, PageRequest.of(filter.getPage(), filter.getSize(), Sort.by(Sort.Direction.DESC, "appointmentDate")));
    }

    @Override
    public void cancelAppointment(Integer id, String reason) {
        Appointment appointment = getAppointmentById(id);
        
        // Business Rule: Can only cancel if not already done/canceled/no_show
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
    public void deleteAppointment(Integer id) {
        Appointment appointment = getAppointmentById(id);
        String status = appointment.getStatus() != null ? appointment.getStatus().toLowerCase() : "";
        if (!"canceled".equals(status)) {
            throw new IllegalStateException("Only canceled appointments can be deleted.");
        }
        appointmentRepository.delete(appointment);
    }

    @Override
    public ByteArrayInputStream exportToExcel(AppointmentFilterRequest filter) {
        // Use the same filter logic but without pagination (or large page size)
        filter.setSize(Integer.MAX_VALUE);
        List<Appointment> appointments = getAppointments(filter).getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Appointments");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Code", "Customer ID", "Pet ID", "Date", "Status", "Note", "Cancel Reason"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
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
    public void assignStaffToAppointment(Integer appointmentId, Integer staffId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!"pending".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("Only pending appointments can be assigned.");
        }

        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        if (!staff.getIsActive()) {
            throw new IllegalStateException("Staff is not active.");
        }

        // Check for schedule conflict
        long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                staffId,
                appointmentId,
                appointment.getAppointmentDate(),
                appointment.getEndTime()
        );

        if (conflictCount > 0) {
            throw new IllegalStateException("Staff is busy in this time slot.");
        }

        appointment.setStaffId(staffId);
        appointment.setStatus("confirmed");
        appointment.setUpdatedAt(java.time.LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    @Override
    public void reassignStaff(Integer appointmentId, Integer newStaffId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!"confirmed".equalsIgnoreCase(appointment.getStatus())) {
            throw new IllegalStateException("Only confirmed appointments can be reassigned.");
        }

        Staff staff = staffRepository.findById(newStaffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + newStaffId));

        if (!staff.getIsActive()) {
            throw new IllegalStateException("Staff is not active.");
        }

        // Check for schedule conflict
        long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                newStaffId,
                appointmentId,
                appointment.getAppointmentDate(),
                appointment.getEndTime()
        );

        if (conflictCount > 0) {
            throw new IllegalStateException("Staff is busy in this time slot.");
        }

        appointment.setStaffId(newStaffId);
        appointment.setUpdatedAt(java.time.LocalDateTime.now());
        appointmentRepository.save(appointment);
    }

    public List<Staff> getAvailableStaffForAppointment(Integer appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);
        List<Staff> activeStaff = staffRepository.findByIsActiveTrue();

        return activeStaff.stream().filter(staff -> {
            long conflictCount = appointmentRepository.countOverlappingStaffAppointments(
                    staff.getStaffId(),
                    appointmentId,
                    appointment.getAppointmentDate(),
                    appointment.getEndTime()
            );
            return conflictCount == 0;
        }).collect(Collectors.toList());
    }
}
