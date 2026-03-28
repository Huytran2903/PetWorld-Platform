package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.config.CustomUserDetails;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.dto.ProfileFormDTO;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryPhotoRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryRepository;
import vn.edu.fpt.petworldplatform.repository.PaymentRepository;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.service.*;
import vn.edu.fpt.petworldplatform.util.SecuritySupport;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;


@Controller
@RequiredArgsConstructor
public class CustomerController {


    private final CustomerService customerService;

    private final SecuritySupport securitySupport;

    private final PetService petService;

    private final FeedbackService feedbackService;
    private final FileStorageService fileStorageService;


    @Autowired
    BookingService bookingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @GetMapping("/profile")
    public String profileShow(Model model) {
        Staff currentStaff = securitySupport.getCurrentAuthenticatedStaff();
        if (currentStaff != null) {
            String roleName = currentStaff.getRole().getRoleName().toUpperCase();
            if ("ADMIN".equals(roleName)) {
                return "redirect:/admin/reports/revenue";
            } else {
                return "redirect:/staff/assigned_list";
            }
        }

        Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();

        if (customerId == null) {
            return "redirect:/login";
        }

        Customer currentFreshUser = customerService.findById(customerId).orElse(null);

        if (currentFreshUser != null) {
            model.addAttribute("user", currentFreshUser);

            boolean canChangePassword = currentFreshUser.getAuthProvider() != AuthProvider.GOOGLE;
            model.addAttribute("hasPassword", canChangePassword);

            return "auth/viewProfile";
        }

        return "redirect:/login";
    }

    @GetMapping("/profile/edit")
    public String profileSetting(Model model) {
        Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();

        if (customerId == null) {
            return "redirect:/login";
        }

        Customer currentFreshUser = customerService.findById(customerId).orElse(null);
        if (currentFreshUser == null) return "redirect:/login?logout";

        ProfileFormDTO form = new ProfileFormDTO();
        form.setFullName(currentFreshUser.getFullName());
        form.setUsername(currentFreshUser.getUsername());
        form.setEmail(currentFreshUser.getEmail());
        form.setPhoneNumber(currentFreshUser.getPhone());

        model.addAttribute("user", form);

        model.addAttribute("isGoogleUser", currentFreshUser.getAuthProvider() == AuthProvider.GOOGLE);

        return "auth/editProfile";
    }

    @PostMapping("/profile/do-edit")
    public String updateProfile(@Valid @ModelAttribute("user") ProfileFormDTO profileForm,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model) {

        Staff currentStaff = securitySupport.getCurrentAuthenticatedStaff();
        if (currentStaff != null) {
            String roleName = currentStaff.getRole().getRoleName().toUpperCase();
            if ("ADMIN".equals(roleName)) {
                return "redirect:/admin/reports/revenue";
            } else {
                return "redirect:/staff/assigned_list";
            }
        }

        Integer customerId = securitySupport.getCurrentAuthenticatedCustomerId();
        if (customerId == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return "auth/editProfile";
        }

        try {
            Customer currentUser = customerService.findById(customerId).orElse(null);

            if (currentUser == null) {
                return "redirect:/login?logout";
            }

            currentUser.setFullName(profileForm.getFullName());
            currentUser.setEmail(profileForm.getEmail());
            currentUser.setPhone(profileForm.getPhoneNumber());

            customerService.updateCustomer(currentUser);

            session.setAttribute("loggedInAccount", currentUser);

            return "redirect:/profile?success";

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Email or phone number has already exist!");
            return "auth/editProfile";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "System error: " + e.getMessage());
            return "auth/editProfile";
        }
    }

    //Order History
    @GetMapping("/customer/order-history")
    public String orderHistory() {
        return "customer/order-history";
    }

    //Cart
    @GetMapping("/customer/shopping-cart")
    public String shoppingCart() {
        return "customer/shopping-cart";
    }

    //Checkout Order
    @GetMapping("/customer/checkout-order")
    public String checkoutOrder() {
        return "customer/checkout-order";
    }


    @Autowired
    private AppointmentSummaryRepository appointmentSummaryRepository;

    @Autowired
    private AppointmentSummaryPhotoRepository appointmentSummaryPhotoRepository;

    @Autowired
    private PetVaccinationRepository petVaccinationRepository;

    @GetMapping("/customer/appointments")
    public String appointmentHistory(HttpSession session,
                                     Model model,
                                     RedirectAttributes redirectAttributes,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        var appointmentPage = bookingService.findAppointmentsPageByCustomerId(
                customer.getCustomerId(),
                PageRequest.of(safePage, safeSize)
        );
        List<Appointment> appointments = appointmentPage.getContent();
        model.addAttribute("appointments", appointments);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", appointmentPage.getTotalPages());
        model.addAttribute("hasPrevious", appointmentPage.hasPrevious());
        model.addAttribute("hasNext", appointmentPage.hasNext());

        // Inline service details: batch load all service lines for these appointments
        List<Integer> apptIds = appointments.stream().map(Appointment::getId).toList();
        if (apptIds.isEmpty()) {
            model.addAttribute("serviceLinesByAppointmentId", java.util.Map.of());
            model.addAttribute("appointmentTotalAmountByAppointmentId", java.util.Map.of());
            model.addAttribute("hasPaidByAppointmentId", java.util.Map.of());
            model.addAttribute("codPendingByAppointmentId", java.util.Map.of());
            model.addAttribute("healthRecordByAppointmentId", java.util.Map.of());
            model.addAttribute("healthPhotosByAppointmentId", java.util.Map.of());
            model.addAttribute("healthRecordByServiceLineId", java.util.Map.of());
            model.addAttribute("healthPhotosByServiceLineId", java.util.Map.of());
            model.addAttribute("reviewedServiceLineIds", java.util.Set.of());
            model.addAttribute("feedbackByLineId", java.util.Map.of());
            model.addAttribute("appointmentSummaryByAppointmentId", java.util.Map.of());
            model.addAttribute("summaryPhotosByAppointmentId", java.util.Map.of());
        } else {
            List<vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine> lines = bookingService.findServiceLinesByAppointmentIds(apptIds);
            java.util.Map<Integer, List<vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine>> linesByApptId =
                    lines.stream().collect(java.util.stream.Collectors.groupingBy(l -> l.getAppointment().getId()));
            model.addAttribute("serviceLinesByAppointmentId", linesByApptId);

            java.util.Map<Integer, java.math.BigDecimal> appointmentTotalAmountByAppointmentId = new java.util.HashMap<>();
            java.util.Map<Integer, Boolean> hasPaidByAppointmentId = new java.util.HashMap<>();
            java.util.Map<Integer, Boolean> codPendingByAppointmentId = new java.util.HashMap<>();

            // Compute totals + payment state for each appointment shown in the list.
            for (Integer apptId : apptIds) {
                List<vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine> linesForAppt = linesByApptId.getOrDefault(apptId, java.util.List.of());

                java.math.BigDecimal total = linesForAppt.stream()
                        .map(line -> {
                            java.math.BigDecimal price = line.getPrice() != null ? line.getPrice() : java.math.BigDecimal.ZERO;
                            Integer qty = line.getQuantity() != null ? line.getQuantity() : 1;
                            return price.multiply(java.math.BigDecimal.valueOf(qty));
                        })
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                appointmentTotalAmountByAppointmentId.put(apptId, total);

                vn.edu.fpt.petworldplatform.entity.Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(
                        apptId,
                        "service"
                );
                boolean paid = latestPayment != null && latestPayment.getPaidAt() != null;
                hasPaidByAppointmentId.put(apptId, paid);

                boolean codPending = latestPayment != null
                        && latestPayment.getPaidAt() == null
                        && latestPayment.getStatus() != null && "pending".equalsIgnoreCase(latestPayment.getStatus())
                        && latestPayment.getMethod() != null && "cod".equalsIgnoreCase(latestPayment.getMethod());
                codPendingByAppointmentId.put(apptId, codPending);
            }

            model.addAttribute("appointmentTotalAmountByAppointmentId", appointmentTotalAmountByAppointmentId);
            model.addAttribute("hasPaidByAppointmentId", hasPaidByAppointmentId);
            model.addAttribute("codPendingByAppointmentId", codPendingByAppointmentId);

            Set<Integer> reviewedServiceLineIds = new HashSet<>();
            java.util.Map<Integer, vn.edu.fpt.petworldplatform.entity.Feedback> feedbackByLineId = new java.util.HashMap<>();
            for (vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine line : lines) {
                if (line.getId() == null || line.getService() == null || line.getService().getId() == null || line.getAppointment() == null) {
                    continue;
                }
                boolean reviewed = feedbackService.hasAlreadyReviewed(
                        line.getAppointment().getId(),
                        line.getService().getId(),
                        customer.getCustomerId()
                );
                if (reviewed) {
                    reviewedServiceLineIds.add(line.getId());
                    feedbackService.getServiceReview(
                            line.getAppointment().getId(),
                            line.getService().getId(),
                            customer.getCustomerId()
                    ).ifPresent(fb -> feedbackByLineId.put(line.getId(), fb));
                }
            }
            model.addAttribute("reviewedServiceLineIds", reviewedServiceLineIds);
            model.addAttribute("feedbackByLineId", feedbackByLineId);

            java.util.Map<Integer, AppointmentSummary> summaryByAppointmentId = new java.util.HashMap<>();
            java.util.Map<Integer, String> serviceStaffByAppointmentId = new java.util.HashMap<>();

            for (Appointment appt : appointments) {
                Integer apptId = appt.getId();
                String fallbackStaff = (appt.getStaff() != null && appt.getStaff().getFullName() != null)
                        ? appt.getStaff().getFullName()
                        : "N/A";

                appointmentSummaryRepository.findByAppointment_Id(apptId).ifPresentOrElse(summary -> {
                    summaryByAppointmentId.put(apptId, summary);

                    String performedStaff = (summary.getSummaryByStaff() != null && summary.getSummaryByStaff().getFullName() != null)
                            ? summary.getSummaryByStaff().getFullName()
                            : fallbackStaff;
                    serviceStaffByAppointmentId.put(apptId, performedStaff);
                }, () -> serviceStaffByAppointmentId.put(apptId, fallbackStaff));
            }

            model.addAttribute("appointmentSummaryByAppointmentId", summaryByAppointmentId);
            model.addAttribute("serviceStaffByAppointmentId", serviceStaffByAppointmentId);

            java.util.Map<Integer, List<AppointmentSummaryPhoto>> summaryPhotosByAppointmentId = new java.util.HashMap<>();
            if (!summaryByAppointmentId.isEmpty()) {
                java.util.Set<Integer> apptIdsForPhotos = summaryByAppointmentId.keySet();
                List<AppointmentSummaryPhoto> allSummaryPhotos =
                        appointmentSummaryPhotoRepository.findAllByAppointmentIdIn(apptIdsForPhotos);
                summaryPhotosByAppointmentId = allSummaryPhotos.stream()
                        .collect(Collectors.groupingBy(p -> p.getSummary().getAppointment().getId()));
            }
            model.addAttribute("summaryPhotosByAppointmentId", summaryPhotosByAppointmentId);

            // Vaccine records (batch)
            Map<Integer, List<vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO>> vaccineRecordsByAppointmentId = new LinkedHashMap<>();
            List<PetVaccinations> vaccineRows = petVaccinationRepository.findByAppointmentIdsWithStaff(apptIds);
            if (!vaccineRows.isEmpty()) {
                vaccineRecordsByAppointmentId = vaccineRows.stream()
                        .filter(v -> v.getAppointment() != null && v.getAppointment().getId() != null)
                        .collect(Collectors.groupingBy(
                                v -> v.getAppointment().getId(),
                                LinkedHashMap::new,
                                Collectors.mapping(v -> vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO.builder()
                                        .vaccineName(v.getVaccineName())
                                        .administeredDate(v.getAdministeredDate())
                                        .nextDueDate(v.getNextDueDate())
                                        .note(v.getNote())
                                        .performedByName(v.getPerformedByStaff() != null ? v.getPerformedByStaff().getFullName() : null)
                                        .build(), Collectors.toList())
                        ));
            }
            model.addAttribute("vaccineRecordsByAppointmentId", vaccineRecordsByAppointmentId);
        }

        return "customer/appointment-history";
    }

    @GetMapping("/customer/appointments/{id}")
    public String appointmentDetail(@PathVariable Integer id,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }

        Appointment appt = bookingService.findAppointmentByIdAndCustomerId(id, customer.getCustomerId())
                .orElse(null);
        if (appt == null) {
            redirectAttributes.addFlashAttribute("error", "Appointment not found.");
            return "redirect:/customer/appointments";
        }

        model.addAttribute("appointment", appt);
        List<AppointmentServiceLine> serviceLines = bookingService.findServiceLinesByAppointmentId(id);
        model.addAttribute("serviceLines", serviceLines);

        java.math.BigDecimal totalAmount = serviceLines.stream()
                .map(line -> {
                    java.math.BigDecimal price = line.getPrice() != null ? line.getPrice() : java.math.BigDecimal.ZERO;
                    Integer qty = line.getQuantity() != null ? line.getQuantity() : 1;
                    return price.multiply(java.math.BigDecimal.valueOf(qty));
                })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(
                appt.getId(),
                "service"
        );
        boolean hasPaid = latestPayment != null && latestPayment.getPaidAt() != null;

        boolean codPending = latestPayment != null
                && latestPayment.getPaidAt() == null
                && latestPayment.getStatus() != null && "pending".equalsIgnoreCase(latestPayment.getStatus())
                && latestPayment.getMethod() != null && "cod".equalsIgnoreCase(latestPayment.getMethod());

        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("hasPaid", hasPaid);
        model.addAttribute("codPending", codPending);

        AppointmentSummary summary = appointmentSummaryRepository.findByAppointment_Id(id).orElse(null);
        model.addAttribute("appointmentSummary", summary);

        List<AppointmentSummaryPhoto> summaryPhotos = summary != null
                ? appointmentSummaryPhotoRepository.findBySummary_Id(summary.getId())
                : List.of();
        model.addAttribute("summaryPhotos", summaryPhotos);

        List<vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO> vaccineRecords = petVaccinationRepository
                .findByAppointmentIdWithStaff(id)
                .stream()
                .map(v -> vn.edu.fpt.petworldplatform.dto.VaccineRecordViewDTO.builder()
                        .vaccineName(v.getVaccineName())
                        .administeredDate(v.getAdministeredDate())
                        .nextDueDate(v.getNextDueDate())
                        .note(v.getNote())
                        .performedByName(v.getPerformedByStaff() != null ? v.getPerformedByStaff().getFullName() : null)
                        .build())
                .toList();
        model.addAttribute("vaccineRecords", vaccineRecords);

        String serviceStaffName = "N/A";
        if (summary != null && summary.getSummaryByStaff() != null && summary.getSummaryByStaff().getFullName() != null) {
            serviceStaffName = summary.getSummaryByStaff().getFullName();
        } else if (appt.getStaff() != null && appt.getStaff().getFullName() != null) {
            serviceStaffName = appt.getStaff().getFullName();
        }
        model.addAttribute("serviceStaffName", serviceStaffName);

        return "customer/appointment-detail";
    }

    @GetMapping("/customer/appointments/{id}/payment")
    public String appointmentPaymentPage(@PathVariable Integer id,
                                         HttpSession session,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }

        Appointment appt = bookingService.findAppointmentByIdAndCustomerId(id, customer.getCustomerId())
                .orElse(null);
        if (appt == null) {
            redirectAttributes.addFlashAttribute("error", "Appointment not found.");
            return "redirect:/customer/appointments";
        }

        if (appt.getStatus() == null || !"done".equalsIgnoreCase(appt.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Payment is only available for completed appointments.");
            return "redirect:/customer/appointments/" + id;
        }

        Payment latestPayment = paymentRepository.findTopByAppointment_IdAndPaymentTypeOrderByCreatedAtDesc(
                appt.getId(), "service");
        boolean hasPaid = latestPayment != null && latestPayment.getPaidAt() != null;
        if (hasPaid) {
            redirectAttributes.addFlashAttribute("message", "This appointment has already been paid.");
            return "redirect:/customer/appointments/" + id;
        }

        List<AppointmentServiceLine> serviceLines = bookingService.findServiceLinesByAppointmentId(id);
        java.math.BigDecimal totalAmount = serviceLines.stream()
                .map(line -> {
                    java.math.BigDecimal price = line.getPrice() != null ? line.getPrice() : java.math.BigDecimal.ZERO;
                    Integer qty = line.getQuantity() != null ? line.getQuantity() : 1;
                    return price.multiply(java.math.BigDecimal.valueOf(qty));
                })
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("appointment", appt);
        model.addAttribute("totalAmount", totalAmount);
        return "customer/appointment-payment";
    }

    @PostMapping("/customer/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Integer id,
                                    @RequestParam String reason,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        try {
            bookingService.cancelAppointment(id, customer.getCustomerId(), reason);
            redirectAttributes.addFlashAttribute("message", "Appointment canceled successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/appointments";
    }

    @PostMapping("/customer/appointments/{id}/reschedule")
    public String rescheduleAppointment(@PathVariable Integer id,
                                        @RequestParam String newDateTime,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        try {
            LocalDateTime parsed;
            try {
                parsed = LocalDateTime.parse(newDateTime);
            } catch (Exception ex) {
                parsed = LocalDateTime.parse(newDateTime.replace(" ", "T"));
            }
            bookingService.rescheduleAppointment(id, customer.getCustomerId(), parsed);
            redirectAttributes.addFlashAttribute("message", "Appointment rescheduled successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Invalid date/time format.");
        }
        return "redirect:/customer/appointments";
    }

    @PostMapping("/customer/appointments/{id}/delete")
    public String deleteCanceledAppointment(@PathVariable Integer id,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        try {
            bookingService.deleteAppointmentIfCanceled(id, customer.getCustomerId());
            redirectAttributes.addFlashAttribute("message", "Appointment deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/appointments";
    }

    @PostMapping("/customer/feedback/{id}/update")
    public String updateFeedback(@PathVariable Integer id,
                                 @RequestParam String comment,
                                 @RequestParam Integer rating,
                                 @RequestParam(required = false) String subject,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        try {
            if (comment == null || comment.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Comment is required.");
                return "redirect:/customer/appointments";
            }
            if (rating == null || rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("error", "Rating must be between 1 and 5.");
                return "redirect:/customer/appointments";
            }
            feedbackService.updateCustomerServiceReview(id, customer.getCustomerId(), comment, rating, subject);
            redirectAttributes.addFlashAttribute("message", "Review updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/appointments";
    }

    @Autowired
    private PetRepo petRepo;

    @GetMapping("/customer/pet/my-pets")
    public String showMyPets(HttpSession session,
                             Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) String petType) {

        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }

        int pageSize = 6;
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, pageSize, Sort.by("petID").descending());

        String normalizedSearch = search == null ? "" : search.trim();
        String selectedPetType = normalizePetTypeFilter(petType);
        String petTypeForQuery = mapPetTypeForQuery(selectedPetType);

        Page<Pets> petPage;
        boolean hasSearch = !normalizedSearch.isEmpty();

        if (petTypeForQuery != null && hasSearch) {
            petPage = petRepo.findByOwner_CustomerIdAndPetTypeAndNameContainingIgnoreCase(
                    customer.getCustomerId(), petTypeForQuery, normalizedSearch, pageable);
        } else if (petTypeForQuery != null) {
            petPage = petRepo.findByOwner_CustomerIdAndPetType(customer.getCustomerId(), petTypeForQuery, pageable);
        } else if (hasSearch) {
            petPage = petRepo.findByOwner_CustomerIdAndNameContainingIgnoreCase(customer.getCustomerId(), normalizedSearch, pageable);
        } else {
            petPage = petRepo.findByOwner_CustomerId(customer.getCustomerId(), pageable);
        }

        List<Pets> myPets = petPage.getContent();

        model.addAttribute("myPets", myPets);
        model.addAttribute("search", normalizedSearch);
        model.addAttribute("selectedPetType", selectedPetType);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", petPage.getTotalPages());
        model.addAttribute("totalItems", petPage.getTotalElements());
        model.addAttribute("hasPrevious", petPage.hasPrevious());
        model.addAttribute("hasNext", petPage.hasNext());

        return "customer/pet/my-pets";
    }

    private String normalizePetTypeFilter(String petType) {
        if (petType == null) {
            return null;
        }
        String value = petType.trim().toLowerCase();
        if (value.isEmpty()) {
            return null;
        }
        if (!value.equals("dog") && !value.equals("cat") && !value.equals("other")) {
            return null;
        }
        return value;
    }

    private String mapPetTypeForQuery(String normalizedPetType) {
        if (normalizedPetType == null) {
            return null;
        }
        return switch (normalizedPetType) {
            case "dog" -> "Dog";
            case "cat" -> "Cat";
            default -> "Other";
        };
    }

    // Backward-compatible mapping: some pages might still link to /customer/pet/create
    @GetMapping({"/customer/pet/pet-create", "/customer/pet/create"})
    public String showPetCreatePage(Model model) {
        model.addAttribute("petDTO", new PetCreateDTO());
        return "customer/pet/pet-create";
    }

    @PostMapping({"/customer/pet/pet-create", "/customer/pet/create"})
    public String handlePetCreateSubmit(HttpSession session,
                                        @ModelAttribute PetCreateDTO petDTO,
                                        RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) return "redirect:/login";

        try {
            // Thiết lập thông tin chủ sở hữu
            petDTO.setCreatePetOwnerType("customer");
            petDTO.setOwnerId(customer.getCustomerId());

            MultipartFile imageFile = petDTO.getImageFile();


            if (imageFile != null && !imageFile.isEmpty()) {
                String secureImageUrl = fileStorageService.storeFile(imageFile);
                petDTO.setImageUrl(secureImageUrl);
            }

            // Lưu thú cưng vào Database
            petService.createPet(petDTO);

            redirectAttributes.addFlashAttribute("message", "Thêm thú cưng thành công!");
            return "redirect:/customer/pet/my-pets";

        } catch (RuntimeException e) {
            // Bắt lỗi bảo mật từ FileStorageService (Sai đuôi file, file hack...)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/pet/pet-create";

        } catch (Exception e) {
            // Bắt các lỗi hệ thống (IOException, Database error...)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/customer/pet/pet-create";
        }
    }

    @GetMapping("/customer/pet/pet-detail")
    public String showPetDetail(@RequestParam("id") Integer id, Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) return "redirect:/login";

        Pets pet = petService.getPetById(id);

        if (!pet.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("pet", pet);
        return "customer/pet/pet-detail";
    }

    @GetMapping("/customer/pet/pet-update")
    public String showPetUpdatePage(@RequestParam("id") Integer id, Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) return "redirect:/login";

        Pets pet = petService.getPetById(id);

        if (!pet.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("pet", pet);
        return "customer/pet/pet-update";
    }

    @PostMapping("/customer/pet/pet-update")
    public String handlePetUpdateSubmit(@ModelAttribute Pets petFromForm,
                                        @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                        RedirectAttributes redirectAttributes
                                        ) {
        try {
            // 1. Lấy thú cưng hiện tại từ Database
            Pets existingPet = petService.getPetById(petFromForm.getPetID());

            // 2. Cập nhật các thông tin cơ bản từ Form
            existingPet.setName(petFromForm.getName());
            existingPet.setAgeMonths(petFromForm.getAgeMonths());
            existingPet.setPetType(petFromForm.getPetType());
            existingPet.setBreed(petFromForm.getBreed());
            existingPet.setWeightKg(petFromForm.getWeightKg());
            existingPet.setColor(petFromForm.getColor());
            existingPet.setNote(petFromForm.getNote());

            existingPet.setOwner(existingPet.getOwner());


            if (imageFile != null && !imageFile.isEmpty()) {
                String secureImageUrl = fileStorageService.storeFile(imageFile);
                existingPet.setImageUrl(secureImageUrl);
            }

            // 4. Lưu lại vào Database
            petService.savePet(existingPet);

            redirectAttributes.addFlashAttribute("message", "Cập nhật thành công!");
            return "redirect:/customer/pet/pet-detail?id=" + existingPet.getPetID();

        } catch (RuntimeException e) {
            // Bắt lỗi bảo mật file (sai đuôi file, sai định dạng MIME...)
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/pet/pet-update?id=" + petFromForm.getPetID();

        } catch (Exception e) {
            // Bắt lỗi hệ thống (IOException, mất kết nối DB...)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật: " + e.getMessage());
            return "redirect:/customer/pet/pet-update?id=" + petFromForm.getPetID();
        }
    }

    @GetMapping("/customer/pet/pet-history")
    public String showPetHistory(@RequestParam("id") Integer id, Model model, HttpSession session) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) return "redirect:/login";

        Pets pet = petService.getPetById(id);
        if (pet == null || pet.getOwner() == null) {
            return "redirect:/customer/pet/my-pets";
        }

        if (!pet.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("pet", pet);

        List<Appointment> historyList = bookingService.findAppointmentsByCustomerAndPet(
                customer.getCustomerId(),
                pet.getPetID()
        );
        model.addAttribute("historyList", historyList);

        List<Integer> appointmentIds = historyList.stream().map(Appointment::getId).toList();
        Map<Integer, String> serviceNamesByAppointmentId = new LinkedHashMap<>();

        if (!appointmentIds.isEmpty()) {
            List<AppointmentServiceLine> lines = bookingService.findServiceLinesByAppointmentIds(appointmentIds);
            serviceNamesByAppointmentId = lines.stream()
                    .filter(line -> line.getAppointment() != null
                            && line.getAppointment().getId() != null
                            && line.getService() != null
                            && line.getService().getName() != null)
                    .collect(Collectors.groupingBy(
                            line -> line.getAppointment().getId(),
                            LinkedHashMap::new,
                            Collectors.mapping(line -> line.getService().getName(), Collectors.joining(", "))
                    ));
        }
        model.addAttribute("serviceNamesByAppointmentId", serviceNamesByAppointmentId);

        return "customer/pet/pet-history";
    }
}

