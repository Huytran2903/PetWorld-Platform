package vn.edu.fpt.petworldplatform.controller;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.InputStreamResource;

import org.springframework.data.domain.Page;

import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.edu.fpt.petworldplatform.dto.AppointmentFilterRequest;

import vn.edu.fpt.petworldplatform.entity.Appointment;

import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;

import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;
import vn.edu.fpt.petworldplatform.entity.ServiceNote;
import vn.edu.fpt.petworldplatform.entity.ServiceNotePhoto;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.PaymentRepository;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryPhotoRepository;
import vn.edu.fpt.petworldplatform.entity.Payment;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceNotePhotoRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceNoteRepository;

import vn.edu.fpt.petworldplatform.service.AppointmentService;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller

@RequestMapping("/admin/appointments")

@RequiredArgsConstructor

public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final ServiceNoteRepository serviceNoteRepository;
    private final ServiceNotePhotoRepository serviceNotePhotoRepository;
    private final AppointmentSummaryPhotoRepository appointmentSummaryPhotoRepository;
    private final PaymentRepository paymentRepository;
    private final PetVaccinationRepository petVaccinationRepository;

    @ModelAttribute("filter")

    public AppointmentFilterRequest appointmentFilterRequest() {

        return new AppointmentFilterRequest();

    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String listAppointments(@ModelAttribute("filter") AppointmentFilterRequest filter, Model model) {

        Page<Appointment> appointmentPage = appointmentService.getAppointments(filter);
        List<Appointment> appointments = appointmentPage.getContent();

        Map<Integer, Boolean> hasPaidByAppointmentId = new LinkedHashMap<>();
        for (Appointment a : appointments) {
            if (a.getId() == null) continue;
            Payment latest = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(a.getId(), "service");
            hasPaidByAppointmentId.put(a.getId(), latest != null && latest.getPaidAt() != null);
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("hasPaidByAppointmentId", hasPaidByAppointmentId);
        model.addAttribute("currentPage", filter.getPage());
        model.addAttribute("totalPages", appointmentPage.getTotalPages());
        model.addAttribute("totalItems", appointmentPage.getTotalElements());

        return "admin/appt-manage";

    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String cancelAppointment(@PathVariable Integer id, @RequestParam String reason,

                                    RedirectAttributes redirectAttributes) {

        try {

            appointmentService.cancelAppointment(id, reason);

            redirectAttributes.addFlashAttribute("message", "Appointment canceled successfully.");

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }

        return "redirect:/admin/appointments/" + id;

    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String deleteAppointment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {

        try {

            appointmentService.deleteAppointment(id);

            redirectAttributes.addFlashAttribute("message", "Appointment deleted successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }

        return "redirect:/admin/appointments";

    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public ResponseEntity<InputStreamResource> exportToExcel(@ModelAttribute AppointmentFilterRequest filter) {

        ByteArrayInputStream in = appointmentService.exportToExcel(filter);

        HttpHeaders headers = new HttpHeaders();

        headers.add("Content-Disposition", "attachment; filename=appointments.xlsx");

        return ResponseEntity.ok()

                .headers(headers)

                .contentType(

                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))

                .body(new InputStreamResource(in));

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String viewDetail(@PathVariable Integer id, Model model,
                             RedirectAttributes redirectAttributes) {

        Appointment appointment;
        try {
            appointment = appointmentService.getAppointmentById(id);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lịch hẹn.");
            return "redirect:/admin/appointments";
        }

        List<AppointmentServiceLine> lines = appointmentService.getServiceLinesByAppointment(id);

        Map<Integer, List<Staff>> availableStaffByLine = new LinkedHashMap<>();

        for (AppointmentServiceLine line : lines) {

            availableStaffByLine.put(

                    line.getId(),

                    appointmentService.getAvailableStaffForServiceLine(id, line.getId())

            );

        }

        // Build unique list of staff who have been assigned to at least one service line
        Map<Integer, Staff> assignedStaffMap = new LinkedHashMap<>();
        for (AppointmentServiceLine line : lines) {
            if (line.getAssignedStaff() != null && line.getAssignedStaff().getStaffId() != null) {
                assignedStaffMap.putIfAbsent(line.getAssignedStaff().getStaffId(), line.getAssignedStaff());
            }
        }

        List<Staff> assignedManagers = new ArrayList<>(assignedStaffMap.values());

        model.addAttribute("appointment", appointment);
        model.addAttribute("serviceLines", lines);
        model.addAttribute("availableStaffByLine", availableStaffByLine);
        model.addAttribute("assignedManagers", assignedManagers);
        model.addAttribute("currentManagerId", appointment.getStaffId());

        AppointmentSummary summary = appointmentSummaryRepository.findByAppointment_Id(id).orElse(null);
        model.addAttribute("appointmentSummary", summary);

        List<AppointmentSummaryPhoto> summaryPhotos = summary != null
                ? appointmentSummaryPhotoRepository.findBySummary_Id(summary.getId())
                : List.of();
        model.addAttribute("summaryPhotos", summaryPhotos);

        List<VaccineRecordViewDTO> vaccineRecords = petVaccinationRepository.findByAppointmentIdWithStaff(id).stream()
                .map(v -> VaccineRecordViewDTO.builder()
                        .vaccineName(v.getVaccineName())
                        .administeredDate(v.getAdministeredDate())
                        .nextDueDate(v.getNextDueDate())
                        .note(v.getNote())
                        .performedByName(v.getPerformedByStaff() != null ? v.getPerformedByStaff().getFullName() : null)
                        .build())
                .toList();
        model.addAttribute("vaccineRecords", vaccineRecords);

        Map<Integer, ServiceNote> serviceNoteByLineId = new LinkedHashMap<>();
        Map<Integer, List<ServiceNotePhoto>> serviceNotePhotosByLineId = new LinkedHashMap<>();
        for (AppointmentServiceLine line : lines) {
            if (line.getId() == null) {
                continue;
            }
            if (line.getAssignedStaffId() == null) {
                continue;
            }
            serviceNoteRepository.findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(id, line.getId(), line.getAssignedStaffId())
                    .ifPresent(note -> {
                        serviceNoteByLineId.put(line.getId(), note);
                        serviceNotePhotosByLineId.put(line.getId(), serviceNotePhotoRepository.findByServiceNote_Id(note.getId()));
                    });
        }
        model.addAttribute("serviceNoteByLineId", serviceNoteByLineId);
        model.addAttribute("serviceNotePhotosByLineId", serviceNotePhotosByLineId);

        Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(id, "service");
        boolean appointmentPaymentPaid = latestPayment != null && latestPayment.getPaidAt() != null;
        BigDecimal totalAmount = lines.stream()
                .map(l -> (l.getPrice() != null ? l.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(l.getQuantity() != null ? l.getQuantity() : 1)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("appointmentPaymentPaid", appointmentPaymentPaid);
        model.addAttribute("appointmentTotalAmount", totalAmount);

        return "admin/appt-detail";

    }

    @PostMapping("/{id}/manager")
    public String updateAppointmentManager(@PathVariable Integer id,
                                           @RequestParam Integer staffId,
                                           RedirectAttributes redirectAttributes) {

        try {

            appointmentService.updateAppointmentManager(id, staffId);

            redirectAttributes.addFlashAttribute("message", "Manager updated successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }

        return "redirect:/admin/appointments/" + id;

    }

    @PostMapping("/{id}/service-lines/{lineId}/assign")
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String assignStaffToLine(@PathVariable Integer id,

                                    @PathVariable Integer lineId,

                                    @RequestParam Integer staffId,

                                    RedirectAttributes redirectAttributes) {

        try {

            appointmentService.assignStaffToServiceLine(id, lineId, staffId);

            redirectAttributes.addFlashAttribute("message", "Service line assigned successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }

        return "redirect:/admin/appointments/" + id;

    }

}
