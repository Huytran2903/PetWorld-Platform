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

import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.edu.fpt.petworldplatform.dto.HealthCheckContextDTO;

import vn.edu.fpt.petworldplatform.dto.SaveHealthReportDraftRequest;

import vn.edu.fpt.petworldplatform.dto.SubmitHealthReportRequest;

import vn.edu.fpt.petworldplatform.dto.UpdateHealthReportRequest;

import vn.edu.fpt.petworldplatform.entity.PetHealthRecord;

import vn.edu.fpt.petworldplatform.entity.Staff;

import vn.edu.fpt.petworldplatform.repository.PetHealthRecordRepository;

import vn.edu.fpt.petworldplatform.service.IHealthCheckService;

import vn.edu.fpt.petworldplatform.service.StaffService;



import java.security.Principal;

import java.util.Optional;



@Controller

@RequestMapping("/staff/health-check")

@RequiredArgsConstructor

public class HealthCheckController {



    private final IHealthCheckService healthCheckService;

    private final StaffService staffService;

    private final PetHealthRecordRepository petHealthRecordRepository;



    @GetMapping("/assigned")

    public String getAssignedAppointments(Principal principal, HttpSession session, Model model) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        model.addAttribute("appointments", healthCheckService.getAssignedAppointments(staff.getStaffId()));

        return "staff/assigned_list";

    }



    @PostMapping("/{appointmentId}/check-in")

    public String checkIn(@PathVariable Integer appointmentId,

                          Principal principal,

                          HttpSession session,

                          RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            healthCheckService.checkInPet(staff.getStaffId(), appointmentId);

            redirectAttributes.addFlashAttribute("message", "Checked in successfully.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }



        return "redirect:/staff/appointment_detail?id=" + appointmentId;

    }



    @PostMapping("/{appointmentId}/execute")

    public String execute(@PathVariable Integer appointmentId,

                          @RequestParam(required = false) Integer serviceLineId,

                          Principal principal,

                          HttpSession session,

                          RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            HealthCheckContextDTO context = (serviceLineId != null)
                    ? healthCheckService.startHealthCheck(staff.getStaffId(), appointmentId, serviceLineId)
                    : healthCheckService.startHealthCheck(staff.getStaffId(), appointmentId);

            return "redirect:/staff/health-check/report?appointmentId=" + appointmentId + "&serviceLineId=" + context.getServiceLineId();

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/staff/appointment_detail?id=" + appointmentId;

        }

    }



    @GetMapping("/report")

    public String viewReportScreen(@RequestParam Integer appointmentId,

                                   @RequestParam(required = false) Integer serviceLineId,

                                   Principal principal,

                                   HttpSession session,

                                   Model model,

                                   RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            HealthCheckContextDTO context = (serviceLineId != null)
                    ? healthCheckService.startHealthCheck(staff.getStaffId(), appointmentId, serviceLineId)
                    : healthCheckService.startHealthCheck(staff.getStaffId(), appointmentId);

            model.addAttribute("context", context);



            SubmitHealthReportRequest submitRequest = new SubmitHealthReportRequest();

            SaveHealthReportDraftRequest draftRequest = new SaveHealthReportDraftRequest();



            petHealthRecordRepository
                    .findByAppointment_IdAndAppointmentServiceLine_Id(appointmentId, context.getServiceLineId())
                    .ifPresent(record -> {

                        mapRecordToSubmitRequest(record, submitRequest);

                        mapRecordToDraftRequest(record, draftRequest);

                    });



            model.addAttribute("submitRequest", submitRequest);

            model.addAttribute("draftRequest", draftRequest);

            return "staff/post-check";

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/staff/appointment_detail?id=" + appointmentId;

        }

    }



    @PostMapping("/{appointmentId}/submit")

    public String submitReport(@PathVariable Integer appointmentId,

                               @RequestParam(required = false) Integer serviceLineId,

                               @ModelAttribute("submitRequest") SubmitHealthReportRequest request,

                               Principal principal,

                               HttpSession session,

                               RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            if (serviceLineId != null) {
                healthCheckService.submitHealthReport(staff.getStaffId(), appointmentId, serviceLineId, request);
            } else {
                healthCheckService.submitHealthReport(staff.getStaffId(), appointmentId, request);
            }

            redirectAttributes.addFlashAttribute("message", "Health report submitted. Appointment moved to done.");

            return "redirect:/staff/appointment_detail?id=" + appointmentId;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            String redirect = "redirect:/staff/health-check/report?appointmentId=" + appointmentId;
            if (serviceLineId != null) {
                redirect += "&serviceLineId=" + serviceLineId;
            }
            return redirect;

        }

    }



    @PostMapping("/{appointmentId}/draft")

    public String saveDraft(@PathVariable Integer appointmentId,

                            @RequestParam(required = false) Integer serviceLineId,

                            @ModelAttribute("draftRequest") SaveHealthReportDraftRequest request,

                            Principal principal,

                            HttpSession session,

                            RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            if (serviceLineId != null) {
                healthCheckService.saveDraft(staff.getStaffId(), appointmentId, serviceLineId, request);
            } else {
                healthCheckService.saveDraft(staff.getStaffId(), appointmentId, request);
            }

            redirectAttributes.addFlashAttribute("message", "Draft saved.");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }



        String redirect = "redirect:/staff/health-check/report?appointmentId=" + appointmentId;
        if (serviceLineId != null) {
            redirect += "&serviceLineId=" + serviceLineId;
        }
        return redirect;

    }



    @PostMapping("/record/{recordId}/update-24h")

    public String updateWithin24h(@PathVariable Integer recordId,

                                  @RequestParam Integer appointmentId,

                                  @ModelAttribute UpdateHealthReportRequest request,

                                  Principal principal,

                                  HttpSession session,

                                  RedirectAttributes redirectAttributes) {

        Staff staff = resolveCurrentStaff(principal, session);

        if (staff == null) {

            return "redirect:/login?error=no_staff_context";

        }



        try {

            healthCheckService.updateWithin24h(staff.getStaffId(), recordId, request);

            redirectAttributes.addFlashAttribute("message", "Health report updated.");

            return "redirect:/staff/appointment_detail?id=" + appointmentId;

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            return "redirect:/staff/health-check/report?appointmentId=" + appointmentId;

        }

    }



    private void mapRecordToSubmitRequest(PetHealthRecord record, SubmitHealthReportRequest submitRequest) {

        if (record.getWeightKg() != null) {

            submitRequest.setWeightKg(record.getWeightKg().doubleValue());

        }

        if (record.getTemperature() != null) {

            submitRequest.setTemperature(record.getTemperature().doubleValue());

        }

        submitRequest.setConditionBefore(record.getConditionBefore());

        submitRequest.setConditionAfter(record.getConditionAfter());

        submitRequest.setFindings(record.getFindings());

        submitRequest.setRecommendations(record.getRecommendations());

        submitRequest.setConditionNotes(record.getNote());

        submitRequest.setWarningFlag(record.getWarningFlag());

    }



    private void mapRecordToDraftRequest(PetHealthRecord record, SaveHealthReportDraftRequest draftRequest) {

        if (record.getWeightKg() != null) {

            draftRequest.setWeightKg(record.getWeightKg().doubleValue());

        }

        if (record.getTemperature() != null) {

            draftRequest.setTemperature(record.getTemperature().doubleValue());

        }

        draftRequest.setConditionBefore(record.getConditionBefore());

        draftRequest.setConditionAfter(record.getConditionAfter());

        draftRequest.setFindings(record.getFindings());

        draftRequest.setRecommendations(record.getRecommendations());

        draftRequest.setConditionNotes(record.getNote());

        draftRequest.setWarningFlag(record.getWarningFlag());

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

