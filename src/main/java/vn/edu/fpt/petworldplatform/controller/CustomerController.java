package vn.edu.fpt.petworldplatform.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.dto.ProfileFormDTO;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummary;
import vn.edu.fpt.petworldplatform.entity.AppointmentSummaryPhoto;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryPhotoRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentSummaryRepository;
import vn.edu.fpt.petworldplatform.entity.*;
import vn.edu.fpt.petworldplatform.repository.PetHealthPhotoRepository;
import vn.edu.fpt.petworldplatform.repository.PetHealthRecordRepository;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.service.*;
import vn.edu.fpt.petworldplatform.util.SecuritySupport;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Controller
@RequiredArgsConstructor
public class CustomerController {


    private final CustomerService customerService;

    private final SecuritySupport securitySupport;

    private final PetService petService;

    private final FeedbackService feedbackService;


    @Autowired
    BookingService bookingService;

    @Autowired
    private PetHealthRecordRepository petHealthRecordRepository;

    @Autowired
    private PetHealthPhotoRepository petHealthPhotoRepository;

    @GetMapping("/profile")
    public String profileShow(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        if (auth.getPrincipal() instanceof Staff) {
            return "redirect:/admin/dashboard";
        }

        Customer authUser = securitySupport.getCurrentAuthenticatedCustomer();
        if (authUser == null) return "redirect:/login";

        Customer currentFreshUser = customerService.findById(authUser.getCustomerId()).orElse(null);

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
        Customer authUser = securitySupport.getCurrentAuthenticatedCustomer();
        if (authUser == null) return "redirect:/login";

        Customer currentFreshUser = customerService.findById(authUser.getCustomerId()).orElse(null);
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

        Customer authUser = securitySupport.getCurrentAuthenticatedCustomer();

        if (authUser == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            return "auth/editProfile";
        }

        try {

            Customer currentUser = customerService.findById(authUser.getCustomerId()).orElse(null);

            if (currentUser == null) {
                return "redirect:/login?logout";
            }

            currentUser.setFullName(profileForm.getFullName());
            currentUser.setEmail(profileForm.getEmail());
            currentUser.setPhone(profileForm.getPhoneNumber());

            customerService.updateCustomer(currentUser);

            session.setAttribute("loggedInAccount", currentUser);

            return "redirect:/profile?success";

        } catch (Exception e) {
            e.printStackTrace();

            String message = e.getMessage();
            if (message != null && (message.contains("Duplicate") || message.contains("UNIQUE"))) {
                model.addAttribute("error", "Email or phone number has already exist!");
            } else {
                model.addAttribute("error", "System error: " + e.getMessage());
            }

            model.addAttribute("user", profileForm);
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

    @GetMapping("/customer/appointments")
    public String appointmentHistory(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute("loggedInAccount");
        if (customer == null) {
            return "redirect:/login";
        }
        List<Appointment> appointments = bookingService.findAppointmentsByCustomerId(customer.getCustomerId());
        model.addAttribute("appointments", appointments);

        // Inline service details: batch load all service lines for these appointments
        List<Integer> apptIds = appointments.stream().map(Appointment::getId).toList();
        if (apptIds.isEmpty()) {
            model.addAttribute("serviceLinesByAppointmentId", java.util.Map.of());
            model.addAttribute("healthRecordByAppointmentId", java.util.Map.of());
            model.addAttribute("healthPhotosByAppointmentId", java.util.Map.of());
            model.addAttribute("healthRecordByServiceLineId", java.util.Map.of());
            model.addAttribute("healthPhotosByServiceLineId", java.util.Map.of());
            model.addAttribute("reviewedServiceLineIds", java.util.Set.of());
            model.addAttribute("appointmentSummaryByAppointmentId", java.util.Map.of());
        } else {
            List<vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine> lines = bookingService.findServiceLinesByAppointmentIds(apptIds);
            java.util.Map<Integer, List<vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine>> linesByApptId =
                    lines.stream().collect(java.util.stream.Collectors.groupingBy(l -> l.getAppointment().getId()));
            model.addAttribute("serviceLinesByAppointmentId", linesByApptId);

            Set<Integer> reviewedServiceLineIds = new HashSet<>();
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
                }
            }
            model.addAttribute("reviewedServiceLineIds", reviewedServiceLineIds);

            java.util.Map<Integer, PetHealthRecord> recordByAppointmentId = new java.util.HashMap<>();
            java.util.Map<Integer, List<PetHealthPhoto>> photosByAppointmentId = new java.util.HashMap<>();
            java.util.Map<Integer, PetHealthRecord> healthRecordByServiceLineId = new java.util.HashMap<>();
            java.util.Map<Integer, List<PetHealthPhoto>> healthPhotosByServiceLineId = new java.util.HashMap<>();
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
        model.addAttribute("serviceLines", bookingService.findServiceLinesByAppointmentId(id));

        AppointmentSummary summary = appointmentSummaryRepository.findByAppointment_Id(id).orElse(null);
        model.addAttribute("appointmentSummary", summary);

        List<AppointmentSummaryPhoto> summaryPhotos = summary != null
                ? appointmentSummaryPhotoRepository.findBySummary_Id(summary.getId())
                : List.of();
        model.addAttribute("summaryPhotos", summaryPhotos);

        String serviceStaffName = "N/A";
        if (summary != null && summary.getSummaryByStaff() != null && summary.getSummaryByStaff().getFullName() != null) {
            serviceStaffName = summary.getSummaryByStaff().getFullName();
        } else if (appt.getStaff() != null && appt.getStaff().getFullName() != null) {
            serviceStaffName = appt.getStaff().getFullName();
        }
        model.addAttribute("serviceStaffName", serviceStaffName);

        return "customer/appointment-detail";
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
            petDTO.setCreatePetOwnerType("customer");
            petDTO.setOwnerId(customer.getCustomerId());

            MultipartFile imageFile = petDTO.getImageFile();
            String imageUrlPath = null;

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();

                String projectDir = System.getProperty("user.dir");

                java.nio.file.Path uploadPath = java.nio.file.Paths.get(projectDir, "src", "main", "resources", "static", "images");

                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }

                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                try (java.io.InputStream inputStream = imageFile.getInputStream()) {
                    java.nio.file.Files.copy(inputStream, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                imageUrlPath = "/images/" + fileName;
                System.out.println("DEBUG: Đã lưu ảnh vào SRC: " + filePath.toAbsolutePath());
            }

            petDTO.setImageUrl(imageUrlPath);
            petService.createPet(petDTO);

            redirectAttributes.addFlashAttribute("message", "Thêm thú cưng thành công!");
            return "redirect:/customer/pet/my-pets";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
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
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
        try {
            Pets existingPet = petService.getPetById(petFromForm.getPetID());

            existingPet.setName(petFromForm.getName());
            existingPet.setAgeMonths(petFromForm.getAgeMonths());
            existingPet.setPetType(petFromForm.getPetType());
            existingPet.setBreed(petFromForm.getBreed());
            existingPet.setWeightKg(petFromForm.getWeightKg());
            existingPet.setColor(petFromForm.getColor());
            existingPet.setNote(petFromForm.getNote());
            existingPet.setOwner(existingPet.getOwner());

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                String projectDir = System.getProperty("user.dir");

                java.nio.file.Path pathSrc = java.nio.file.Paths.get(projectDir, "src", "main", "resources", "static", "images");
                if (!java.nio.file.Files.exists(pathSrc)) {
                    java.nio.file.Files.createDirectories(pathSrc);
                }
                java.nio.file.Path fileSrc = pathSrc.resolve(fileName);

                try (java.io.InputStream inputStream = imageFile.getInputStream()) {
                    java.nio.file.Files.copy(inputStream, fileSrc, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                java.nio.file.Path pathTarget = java.nio.file.Paths.get(projectDir, "target", "classes", "static", "images");
                if (!java.nio.file.Files.exists(pathTarget)) {
                    java.nio.file.Files.createDirectories(pathTarget);
                }
                java.nio.file.Path fileTarget = pathTarget.resolve(fileName);
                java.nio.file.Files.copy(fileSrc, fileTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                existingPet.setImageUrl("/images/" + fileName);

                System.out.println("DEBUG: Đã lưu ảnh vào cả SRC và TARGET: " + fileName);
            }

            petService.savePet(existingPet);

            redirectAttributes.addFlashAttribute("message", "Cập nhật thành công!");
            return "redirect:/customer/pet/pet-detail?id=" + existingPet.getPetID();

        } catch (Exception e) {
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

        if (!pet.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            return "redirect:/access-denied";
        }

        model.addAttribute("pet", pet);

        List<Appointment> historyList = new ArrayList<>();
        model.addAttribute("historyList", historyList);

        return "customer/pet/pet-history";
    }
}

