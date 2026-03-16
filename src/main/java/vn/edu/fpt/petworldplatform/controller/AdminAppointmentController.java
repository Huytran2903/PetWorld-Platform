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

import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;

import vn.edu.fpt.petworldplatform.entity.Staff;

import vn.edu.fpt.petworldplatform.repository.PetHealthPhotoRepository;

import vn.edu.fpt.petworldplatform.repository.PetHealthRecordRepository;

import vn.edu.fpt.petworldplatform.service.AppointmentService;

import java.io.ByteArrayInputStream;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

@Controller

@RequestMapping("/admin/appointments")

@RequiredArgsConstructor

public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    private final PetHealthRecordRepository petHealthRecordRepository;

    private final PetHealthPhotoRepository petHealthPhotoRepository;

    @ModelAttribute("filter")

    public AppointmentFilterRequest appointmentFilterRequest() {

        return new AppointmentFilterRequest();

    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_APPOINTMENT')")
    public String listAppointments(@ModelAttribute("filter") AppointmentFilterRequest filter, Model model) {

        Page<Appointment> appointmentPage = appointmentService.getAppointments(filter);

        model.addAttribute("appointments", appointmentPage.getContent());

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
    public String viewDetail(@PathVariable Integer id, Model model) {

        Appointment appointment = appointmentService.getAppointmentById(id);

        List<AppointmentServiceLine> lines = appointmentService.getServiceLinesByAppointment(id);

        Map<Integer, List<Staff>> availableStaffByLine = new LinkedHashMap<>();

        for (AppointmentServiceLine line : lines) {

            availableStaffByLine.put(

                    line.getId(),

                    appointmentService.getAvailableStaffForServiceLine(id, line.getId())

            );

        }

        model.addAttribute("appointment", appointment);

        model.addAttribute("serviceLines", lines);

        model.addAttribute("availableStaffByLine", availableStaffByLine);

        PetHealthRecord healthRecord = petHealthRecordRepository.findTopByAppointment_IdOrderByUpdatedAtDesc(id)

                .orElse(null);

        model.addAttribute("healthRecord", healthRecord);

        model.addAttribute("healthPhotos", healthRecord == null

                ? java.util.List.of()

                : petHealthPhotoRepository.findByRecord_Id(healthRecord.getId()));

        Map<Integer, PetHealthRecord> healthRecordByServiceLineId = new LinkedHashMap<>();
        Map<Integer, List<vn.edu.fpt.petworldplatform.entity.PetHealthPhoto>> healthPhotosByServiceLineId = new LinkedHashMap<>();
        for (AppointmentServiceLine line : lines) {
            if (line.getId() == null) {
                continue;
            }
            petHealthRecordRepository.findByAppointmentServiceLine_Id(line.getId())
                    .ifPresent(record -> {
                        healthRecordByServiceLineId.put(line.getId(), record);
                        healthPhotosByServiceLineId.put(line.getId(),
                                petHealthPhotoRepository.findByRecord_Id(record.getId()));
                    });
        }
        model.addAttribute("healthRecordByServiceLineId", healthRecordByServiceLineId);
        model.addAttribute("healthPhotosByServiceLineId", healthPhotosByServiceLineId);

        return "admin/appt-detail";

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
