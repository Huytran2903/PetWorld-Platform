package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.ServiceNote;
import vn.edu.fpt.petworldplatform.entity.ServiceNotePhoto;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryPhotoRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryRepository;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceNotePhotoRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceNoteRepository;
import vn.edu.fpt.petworldplatform.service.IAssignedAppointmentService;
import vn.edu.fpt.petworldplatform.service.StaffService;
import vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO;

import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final IAssignedAppointmentService assignedAppointmentService;
    private final StaffService staffService;
    private final ServiceNoteRepository serviceNoteRepository;
    private final ServiceNotePhotoRepository serviceNotePhotoRepository;
    private final AppointmentSummaryRepository appointmentSummaryRepository;
    private final AppointmentSummaryPhotoRepository appointmentSummaryPhotoRepository;
    private final PetVaccinationRepository petVaccinationRepository;

    @GetMapping("/assigned_list")
    public String viewAssignedList(
            Principal principal,
            HttpSession session,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status,
            Model model) {

        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        session.setAttribute("currentStaffId", staff.getStaffId());

        model.addAttribute("appointments",
                assignedAppointmentService.getAssignedAppointments(staff.getStaffId(), date, status));
        model.addAttribute("date", date);
        model.addAttribute("status", status);
        model.addAttribute("staffName", staff.getFullName());
        model.addAttribute("currentStaffId", staff.getStaffId());

        return "staff/assigned_list";
    }

    @GetMapping("/appointment_detail")
    public String viewAppointmentDetail(@RequestParam Integer id,
                                        Principal principal,
                                        HttpSession session,
                                        Model model) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        session.setAttribute("currentStaffId", staff.getStaffId());

        Appointment appointment = assignedAppointmentService.getAppointmentDetail(staff.getStaffId(), id);
        model.addAttribute("appointment", appointment);
        model.addAttribute("currentStaffId", staff.getStaffId());

        boolean isManager = appointment.getStaffId() != null && appointment.getStaffId().equals(staff.getStaffId());

        // Nếu là manager: xem được tất cả service lines trong appointment.
        // Nếu không: chỉ xem các service line được assign cho chính mình.
        List<AppointmentServiceLine> myServiceLines;
        if (appointment.getServiceLines() == null) {
            myServiceLines = List.of();
        } else if (isManager) {
            myServiceLines = appointment.getServiceLines();
        } else {
            myServiceLines = appointment.getServiceLines().stream()
                    .filter(line -> line.getAssignedStaffId() != null && line.getAssignedStaffId().equals(staff.getStaffId()))
                    .toList();
        }
        model.addAttribute("myServiceLines", myServiceLines);

        Map<Integer, ServiceNote> serviceNoteByLineId = new HashMap<>();
        Map<Integer, List<ServiceNotePhoto>> serviceNotePhotosByLineId = new HashMap<>();

        if (isManager) {
            // Manager: lấy note mới nhất cho MỖI service line trong appointment (bất kể note đó do staff nào viết).
            List<ServiceNote> allNotes = serviceNoteRepository.findByAppointment_IdOrderByUpdatedAtDesc(id);
            for (ServiceNote note : allNotes) {
                if (note.getServiceLine() == null || note.getServiceLine().getId() == null) {
                    continue;
                }
                Integer lineId = note.getServiceLine().getId();
                if (serviceNoteByLineId.containsKey(lineId)) {
                    // đã có note mới hơn cho line này rồi
                    continue;
                }
                serviceNoteByLineId.put(lineId, note);
                serviceNotePhotosByLineId.put(lineId, serviceNotePhotoRepository.findByServiceNote_Id(note.getId()));
            }
        } else {
            // Staff bình thường: chỉ xem note của chính mình cho các line được assign.
            for (AppointmentServiceLine line : myServiceLines) {
                if (line.getId() == null) {
                    continue;
                }
                serviceNoteRepository.findByAppointment_IdAndServiceLine_IdAndStaff_StaffId(id, line.getId(), staff.getStaffId())
                        .ifPresent(note -> {
                            serviceNoteByLineId.put(line.getId(), note);
                            serviceNotePhotosByLineId.put(line.getId(), serviceNotePhotoRepository.findByServiceNote_Id(note.getId()));
                        });
            }
        }

        model.addAttribute("serviceNoteByLineId", serviceNoteByLineId);
        model.addAttribute("serviceNotePhotosByLineId", serviceNotePhotosByLineId);

        // Vaccine records for this appointment (for summary view)
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

        Map<Integer, VaccineRecordViewDTO> vaccineRecordByLineId = new LinkedHashMap<>();
        if (isManager && !myServiceLines.isEmpty()) {
            List<PetVaccinations> vaccineRecordRows = petVaccinationRepository.findByAppointmentIdWithStaff(id);
            if (!vaccineRecordRows.isEmpty()) {
                for (AppointmentServiceLine line : myServiceLines) {
                    if (line == null || line.getId() == null || line.getService() == null) continue;
                    String serviceType = line.getService().getServiceType();
                    if (serviceType == null) continue;
                    String st = serviceType.trim().toLowerCase(Locale.ROOT);
                    if (!"vaccine".equals(st) && !"vaccination".equals(st)) continue;

                    String targetName = line.getService().getName() != null ? line.getService().getName().trim() : "";
                    if (targetName.isEmpty()) continue;

                    PetVaccinations matched = vaccineRecordRows.stream()
                            .filter(v -> v.getVaccineName() != null && v.getVaccineName().trim().equalsIgnoreCase(targetName))
                            .findFirst()
                            .orElse(null);
                    if (matched == null) continue;

                    vaccineRecordByLineId.put(line.getId(), VaccineRecordViewDTO.builder()
                            .vaccineName(matched.getVaccineName())
                            .administeredDate(matched.getAdministeredDate())
                            .nextDueDate(matched.getNextDueDate())
                            .note(matched.getNote())
                            .performedByName(matched.getPerformedByStaff() != null ? matched.getPerformedByStaff().getFullName() : null)
                            .build());
                }
            }
        }
        model.addAttribute("vaccineRecordByLineId", vaccineRecordByLineId);

        // Flatten all service note photos for manager to pick as evidence
        List<ServiceNotePhoto> allServiceNotePhotos = serviceNotePhotosByLineId.values().stream()
                .flatMap(List::stream)
                .toList();
        model.addAttribute("allServiceNotePhotos", allServiceNotePhotos);

        AppointmentSummary appointmentSummary = appointmentSummaryRepository.findByAppointment_Id(id).orElse(null);
        model.addAttribute("appointmentSummary", appointmentSummary);

        List<AppointmentSummaryPhoto> summaryPhotos = appointmentSummary != null
                ? appointmentSummaryPhotoRepository.findBySummary_Id(appointmentSummary.getId())
                : List.of();
        model.addAttribute("summaryPhotos", summaryPhotos);
        model.addAttribute("isManager", isManager);

        return "staff/appointment_detail";
    }

    @PostMapping("/appointment/checkin")
    public String checkIn(@RequestParam Integer id,
                          Principal principal,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        try {
            assignedAppointmentService.checkIn(staff.getStaffId(), id);
            redirectAttributes.addFlashAttribute("message", "Checked in successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/staff/appointment_detail?id=" + id;
    }

    @PostMapping("/appointment/no-show")
    public String reportNoShow(@RequestParam Integer id,
                               Principal principal,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Staff staff = resolveCurrentStaff(principal, session);
        if (staff == null) {
            return "redirect:/login?error=no_staff_context";
        }

        try {
            assignedAppointmentService.reportNoShow(staff.getStaffId(), id);
            redirectAttributes.addFlashAttribute("message", "Appointment marked as No Show.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/staff/appointment_detail?id=" + id;
    }

    @GetMapping("/booking")
    public String staffBooking() {
        return "redirect:/booking";
    }

    private Staff resolveCurrentStaff(Principal principal, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken authToken) {
            Object principalObj = authToken.getPrincipal();
            if (principalObj instanceof Staff staff) {
                return staff;
            }
            if (principalObj instanceof UserDetails userDetails) {
                Staff byUsername = staffService.findByUsername(userDetails.getUsername()).orElse(null);
                if (byUsername != null) {
                    return byUsername;
                }
            }
        }

        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            String email = oauth2Token.getPrincipal().getAttribute("email");
            if (email != null && !email.isBlank()) {
                Optional<Staff> byEmail = staffService.findByEmail(email);
                if (byEmail.isPresent()) {
                    return byEmail.get();
                }
            }
        }

        if (principal != null) {
            Staff byPrincipal = staffService.findByUsername(principal.getName()).orElse(null);
            if (byPrincipal != null) {
                return byPrincipal;
            }
        }

        Object loggedInStaff = session.getAttribute("loggedInStaff");
        if (loggedInStaff instanceof Staff staff) {
            return staff;
        }

        Object currentStaffId = session.getAttribute("currentStaffId");
        if (currentStaffId instanceof Long idFromSession) {
            return staffService.findById(idFromSession).orElse(null);
        }
        if (currentStaffId instanceof Integer idFromSessionInt) {
            return staffService.findById(idFromSessionInt.longValue()).orElse(null);
        }

        return null;
    }
}
