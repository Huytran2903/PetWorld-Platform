package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.NotificationRepository;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int LEAD_TIME_HOURS = 2;
    private static final int MAX_ADVANCE_BOOKING_DAYS = 30;
    private static final int CANCEL_RESCHEDULE_MIN_HOURS = 1;
    private static final int MAX_RESCHEDULE_TIMES = 2;
    private static final int RESCHEDULE_COOLDOWN_MINUTES = 30;
    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(20, 0);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;
    private final CustomerRepo customerRepo;
    private final NotificationRepository notificationRepository;
    private final PetRepo petRepo;
    private final ServiceItemRepository serviceItemRepository;
    private final PetVaccinationRepository petVaccinationRepository;

    public List<Pets> findPetsByCustomerId(Integer customerId) {
        return petRepo.findByOwner_CustomerId(customerId);
    }

    public List<ServiceItem> findActiveServices() {
        return serviceItemRepository.findAllByOrderByServiceTypeAscNameAsc().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();
    }

    public List<ServiceItem> findActiveServicesByType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return findActiveServices();
        }
        return serviceItemRepository.findByServiceTypeIgnoreCaseOrderByNameAsc(serviceType.trim()).stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .toList();
    }

    /**
     * BR-17: Booking must be at least 2 hours in advance.
     */
    public Optional<String> validateAppointmentDateTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        if (dateTime.isBefore(now.plusHours(LEAD_TIME_HOURS))) {
            return Optional.of("Booking must be at least 2 hours in advance.");
        }
        if (dateTime.isAfter(now.plusDays(MAX_ADVANCE_BOOKING_DAYS))) {
            return Optional.of("Booking can only be made up to 30 days in advance.");
        }
        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(OPEN_TIME) || !time.isBefore(CLOSE_TIME)) {
            return Optional.of("Selected time is outside operating hours (08:00 - 20:00).");
        }
        return Optional.empty();
    }

    /**
     * Validate operating hours for a time range.
     */
    public void validateOperatingHoursRange(LocalDateTime start, LocalDateTime end, boolean hasBoarding) {
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        // Check start time
        if (startTime.isBefore(OPEN_TIME) || !startTime.isBefore(CLOSE_TIME)) {
            throw new IllegalArgumentException("New time violates operating hours / lead time.");
        }

        // Check if it crosses to next day
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            if (!hasBoarding) {
                throw new IllegalArgumentException("New time slot does not fit the required service duration (crosses to next day).");
            }
            // For boarding, we assume it's allowed but could add specific cross-day rules here if needed
        } else {
            // Same day - check end time
            if (endTime.isAfter(CLOSE_TIME)) {
                throw new IllegalArgumentException("New time slot does not fit the required service duration (ends after closing).");
            }
        }
    }

    public Optional<Appointment> findById(Integer id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public Appointment createAppointment(Integer customerId, Integer petId, LocalDateTime appointmentDate,
                                         String note, List<Integer> serviceIds) {
        String code = generateAppointmentCode();
        
        List<ServiceItem> services = serviceItemRepository.findAllById(serviceIds);
        validateVaccineEligibilityOrThrow(petId, appointmentDate.toLocalDate(), services);
        int totalDuration = 0;
        boolean hasBoarding = false;
        
        for (ServiceItem svc : services) {
            if (Boolean.TRUE.equals(svc.getIsActive())) {
                totalDuration += (svc.getDurationMinutes() != null ? svc.getDurationMinutes() : 30);
                if ("boarding".equalsIgnoreCase(svc.getServiceType()) || (svc.getDurationMinutes() != null && svc.getDurationMinutes() >= 1440)) {
                    hasBoarding = true;
                }
            }
        }

        LocalDateTime endTime = appointmentDate.plusMinutes(totalDuration);
        validateOperatingHoursRange(appointmentDate, endTime, hasBoarding);

        // Check overlap for new appointment
        if (appointmentRepository.countOverlappingAppointments(petId, -1, appointmentDate, endTime) > 0) {
            throw new IllegalArgumentException("New time slot overlaps with an existing booking.");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found."));
        Pets pet = petRepo.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found."));

        Appointment appointment = Appointment.builder()
                .appointmentCode(code)
                .customer(customer)
                .pet(pet)
                .appointmentDate(appointmentDate)
                .endTime(endTime)
                .note(note)
                .status("pending")
                .build();
        appointment = appointmentRepository.save(appointment);

        for (ServiceItem svc : services) {
            if (Boolean.TRUE.equals(svc.getIsActive())) {
                AppointmentServiceLine line = AppointmentServiceLine.builder()
                        .appointment(appointment)
                        .service(svc)
                        .price(svc.getBasePrice())
                        .quantity(1)
                        .build();
                appointmentServiceLineRepository.save(line);
            }
        }
        return appointment;
    }

    /**
     * Rule: Once a pet has been vaccinated for a vaccine type, it cannot book that same vaccine
     * again until the latest NextDueDate (inclusive). If NextDueDate is NULL, we treat it as
     * "not eligible again" (lock indefinitely) to avoid duplicate bookings.
     */
    private void validateVaccineEligibilityOrThrow(Integer petId, LocalDate appointmentDate, List<ServiceItem> services) {
        if (petId == null || appointmentDate == null || services == null || services.isEmpty()) return;

        for (ServiceItem svc : services) {
            if (svc == null) continue;
            if (!isVaccineService(svc)) continue;

            String vaccineName = (svc.getName() != null) ? svc.getName().trim() : "";
            if (vaccineName.isEmpty()) continue;

            Optional<PetVaccinations> latestOpt =
                    petVaccinationRepository.findTopByPet_PetIDAndVaccineNameIgnoreCaseOrderByAdministeredDateDescCreatedAtDesc(
                            petId, vaccineName
                    );

            if (latestOpt.isEmpty()) continue; // never vaccinated => eligible

            PetVaccinations latest = latestOpt.get();
            LocalDate lockUntil = latest.getNextDueDate(); // may be null

            // If nextDueDate is null, lock indefinitely (must be handled by staff/admin to set next due)
            if (lockUntil == null) {
                throw new IllegalArgumentException("This pet has already received vaccine '" + vaccineName + "'. It cannot be booked again (missing next due date).");
            }

            // Eligible only when appointmentDate is on/after nextDueDate
            if (appointmentDate.isBefore(lockUntil)) {
                throw new IllegalArgumentException(
                        "Vaccine '" + vaccineName + "' is not eligible until " + lockUntil.format(DateTimeFormatter.ISO_DATE) + "."
                );
            }
        }
    }

    private boolean isVaccineService(ServiceItem svc) {
        String t = svc.getServiceType();
        if (t == null) return false;
        String type = t.trim().toLowerCase(Locale.ROOT);
        return "vaccine".equals(type) || "vaccination".equals(type);
    }

    private String generateAppointmentCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String shortUuid = uuid.substring(0, 12);
        return "APT-" + shortUuid.toUpperCase();
    }

    /**
     * Customer appointment list: pending first, then all others ordered by appointment date (newest first).
     */
    public List<Appointment> findAppointmentsByCustomerId(Integer customerId) {
        List<Appointment> list = appointmentRepository.findByCustomerIdOrderByAppointmentDateDesc(customerId);
        list.sort(Comparator
                .comparing((Appointment a) -> !isPendingAppointmentStatus(a.getStatus()))
                .thenComparing(Appointment::getAppointmentDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    private static boolean isPendingAppointmentStatus(String status) {
        return status != null && "pending".equalsIgnoreCase(status.trim());
    }

    public List<Appointment> findActiveAppointmentsByCustomerId(Integer customerId) {
        List<String> activeStatuses = List.of("pending", "confirmed");
        return appointmentRepository.findByCustomerIdAndStatusInOrderByAppointmentDateDesc(customerId, activeStatuses);
    }

    public Optional<Appointment> findAppointmentByIdAndCustomerId(Integer id, Integer customerId) {
        return appointmentRepository.findById(id)
                .filter(a -> a.getCustomerId().equals(customerId));
    }

    public List<AppointmentServiceLine> findServiceLinesByAppointmentId(Integer appointmentId) {
        return appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
    }

    public List<AppointmentServiceLine> findServiceLinesByAppointmentIds(List<Integer> appointmentIds) {
        if (appointmentIds == null || appointmentIds.isEmpty()) return List.of();
        return appointmentServiceLineRepository.findAllByAppointmentIdsWithService(appointmentIds);
    }

    public Optional<String> canCancelOrReschedule(Integer appointmentId, Integer customerId) {
        Optional<Appointment> opt = findAppointmentByIdAndCustomerId(appointmentId, customerId);
        if (opt.isEmpty()) return Optional.of("Appointment not found.");
        Appointment a = opt.get();
        if (!List.of("pending", "confirmed").contains(a.getStatus())) {
            return Optional.of("Cannot cancel/reschedule an appointment with status: " + a.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (a.getAppointmentDate().isBefore(now.plusHours(CANCEL_RESCHEDULE_MIN_HOURS))) {
            return Optional.of("Cannot reschedule within 1 hour of the appointment.");
        }
        return Optional.empty();
    }

    public Optional<String> canReschedule(Integer appointmentId, Integer customerId) {
        Optional<String> baseValidationError = canCancelOrReschedule(appointmentId, customerId);
        if (baseValidationError.isPresent()) {
            return baseValidationError;
        }

        Appointment a = findAppointmentByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        int currentRescheduleCount = a.getRescheduleCount() != null ? a.getRescheduleCount() : 0;
        if (currentRescheduleCount >= MAX_RESCHEDULE_TIMES) {
            return Optional.of("You have reached the maximum number of reschedules (2 times).");
        }

        LocalDateTime now = LocalDateTime.now();
        if (a.getLastRescheduledAt() != null
                && a.getLastRescheduledAt().plusMinutes(RESCHEDULE_COOLDOWN_MINUTES).isAfter(now)) {
            return Optional.of("You can only reschedule once every 30 minutes.");
        }

        return Optional.empty();
    }

    @Transactional
    public void cancelAppointment(Integer appointmentId, Integer customerId, String reason) {
        Optional<String> err = canCancelOrReschedule(appointmentId, customerId);
        if (err.isPresent()) throw new IllegalArgumentException(err.get());
        Appointment a = findAppointmentByIdAndCustomerId(appointmentId, customerId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        a.setStatus("canceled");
        a.setCancellationReason(reason);
        a.setCanceledAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(a);
    }

    public int computeAppointmentDurationMinutes(List<AppointmentServiceLine> lines) {
        return lines.stream()
                .mapToInt(line -> (line.getService().getDurationMinutes() != null ? line.getService().getDurationMinutes() : 30) * (line.getQuantity() != null ? line.getQuantity() : 1))
                .sum();
    }

    @Transactional
    public void rescheduleAppointment(Integer appointmentId, Integer customerId, LocalDateTime newStart) {
        Optional<String> err = canReschedule(appointmentId, customerId);
        if (err.isPresent()) {
            // Ensure we use the exact BR-18 message from user requirement
            if (err.get().contains("within 1 hour")) {
                throw new IllegalArgumentException("Cannot reschedule within 1 hour of the appointment.");
            }
            throw new IllegalArgumentException(err.get());
        }

        Appointment a = findAppointmentByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // 1) Compute totalDurationMinutes
        List<AppointmentServiceLine> lines = appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
        int totalMinutes = computeAppointmentDurationMinutes(lines);
        boolean hasBoarding = lines.stream().anyMatch(l -> 
            "boarding".equalsIgnoreCase(l.getService().getServiceType()) || 
            (l.getService().getDurationMinutes() != null && l.getService().getDurationMinutes() >= 1440));

        // 2) Define newEnd
        LocalDateTime newEnd = newStart.plusMinutes(totalMinutes);

        // 3) Validate operating hours and duration fit
        validateOperatingHoursRange(newStart, newEnd, hasBoarding);

        // 4) Lead time check (reusing existing method logic but for newStart)
        Optional<String> leadTimeErr = validateAppointmentDateTime(newStart);
        if (leadTimeErr.isPresent()) {
            throw new IllegalArgumentException("New time violates operating hours / lead time.");
        }

        // 5) Prevent overlaps
        if (appointmentRepository.countOverlappingAppointments(a.getPetId(), a.getId(), newStart, newEnd) > 0) {
            throw new IllegalArgumentException("New time slot overlaps with an existing booking.");
        }

        // 6) Persist
        a.setPreviousAppointmentDate(a.getAppointmentDate());
        a.setAppointmentDate(newStart);
        a.setEndTime(newEnd);
        a.setRescheduledAt(LocalDateTime.now());
        a.setLastRescheduledAt(LocalDateTime.now());
        a.setRescheduleCount((a.getRescheduleCount() != null ? a.getRescheduleCount() : 0) + 1);
        a.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(a);
    }

    // NEW ---------------------------------------------
    /**
     * Delete a canceled appointment owned by customer.
     */
    @Transactional
    public void deleteAppointmentIfCanceled(Integer appointmentId, Integer customerId) {
        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (!a.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Unauthorized to delete this appointment.");
        }
        if (!"canceled".equalsIgnoreCase(a.getStatus())) {
            throw new IllegalArgumentException("Only canceled appointments can be deleted.");
        }

        // Cách 1: xóa bản ghi phụ thuộc trước, sau đó mới xóa appointment
        notificationRepository.deleteByAppointment_Id(appointmentId);
        appointmentServiceLineRepository.deleteAllByAppointment(a);
        appointmentRepository.delete(a);
    }
}
