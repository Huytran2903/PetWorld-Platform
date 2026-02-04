package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Pet;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.PetRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int LEAD_TIME_HOURS = 2;
    private static final int CANCEL_RESCHEDULE_MIN_HOURS = 1;
    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(20, 0);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;
    private final PetRepository petRepository;
    private final ServiceItemRepository serviceItemRepository;

    public List<Pet> findPetsByCustomerId(Integer customerId) {
        return petRepository.findByOwner_CustomerId(customerId);
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
        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(OPEN_TIME) || !time.isBefore(CLOSE_TIME)) {
            return Optional.of("Selected time is outside operating hours (08:00 - 20:00).");
        }
        return Optional.empty();
    }

    public Optional<Appointment> findById(Integer id) {
        return appointmentRepository.findById(id);
    }

    @Transactional
    public Appointment createAppointment(Long customerId, Integer petId, LocalDateTime appointmentDate,
                                         String note, List<Integer> serviceIds) {
        String code = generateAppointmentCode();
        Appointment appointment = Appointment.builder()
                .appointmentCode(code)
                .customerId(customerId)
                .petId(petId)
                .appointmentDate(appointmentDate)
                .note(note)
                .status("pending")
                .build();
        appointment = appointmentRepository.save(appointment);

        List<ServiceItem> services = serviceItemRepository.findAllById(serviceIds);
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

    private String generateAppointmentCode() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String shortUuid = uuid.substring(0, 12);
        return "APT-" + shortUuid.toUpperCase();
    }

    public List<Appointment> findAppointmentsByCustomerId(Long customerId) {
        return appointmentRepository.findByCustomerIdOrderByAppointmentDateDesc(customerId);
    }

    public List<Appointment> findActiveAppointmentsByCustomerId(Long customerId) {
        List<String> activeStatuses = List.of("pending", "confirmed");
        return appointmentRepository.findByCustomerIdAndStatusInOrderByAppointmentDateDesc(customerId, activeStatuses);
    }

    public Optional<Appointment> findAppointmentByIdAndCustomerId(Integer id, Long customerId) {
        return appointmentRepository.findById(id)
                .filter(a -> a.getCustomerId().equals(customerId));
    }

    public List<AppointmentServiceLine> findServiceLinesByAppointmentId(Integer appointmentId) {
        return appointmentServiceLineRepository.findByAppointment_Id(appointmentId);
    }

    public Optional<String> canCancelOrReschedule(Integer appointmentId, Long customerId) {
        Optional<Appointment> opt = findAppointmentByIdAndCustomerId(appointmentId, customerId);
        if (opt.isEmpty()) return Optional.of("Appointment not found.");
        Appointment a = opt.get();
        if (!List.of("pending", "confirmed").contains(a.getStatus())) {
            return Optional.of("Cannot cancel/reschedule an appointment with status: " + a.getStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        if (a.getAppointmentDate().isBefore(now.plusHours(CANCEL_RESCHEDULE_MIN_HOURS))) {
            return Optional.of("Cannot cancel/reschedule within 1 hour of appointment.");
        }
        return Optional.empty();
    }

    @Transactional
    public void cancelAppointment(Integer appointmentId, Long customerId, String reason) {
        Optional<String> err = canCancelOrReschedule(appointmentId, customerId);
        if (err.isPresent()) throw new IllegalArgumentException(err.get());
        Appointment a = findAppointmentByIdAndCustomerId(appointmentId, customerId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        a.setStatus("canceled");
        a.setCancellationReason(reason);
        a.setCanceledAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(a);
    }

    @Transactional
    public void rescheduleAppointment(Integer appointmentId, Long customerId, LocalDateTime newDateTime) {
        Optional<String> err = canCancelOrReschedule(appointmentId, customerId);
        if (err.isPresent()) throw new IllegalArgumentException(err.get());
        Appointment a = findAppointmentByIdAndCustomerId(appointmentId, customerId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        Optional<String> validation = validateAppointmentDateTime(newDateTime);
        if (validation.isPresent()) throw new IllegalArgumentException(validation.get());
        a.setPreviousAppointmentDate(a.getAppointmentDate());
        a.setAppointmentDate(newDateTime);
        a.setRescheduledAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(a);
    }

    // NEW ---------------------------------------------
    /**
     * Delete a canceled appointment owned by customer.
     */
    @Transactional
    public void deleteAppointmentIfCanceled(Integer appointmentId, Long customerId) {
        Appointment a = appointmentRepository.findById(appointmentId).orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!a.getCustomerId().equals(customerId)) throw new IllegalArgumentException("Unauthorized to delete this appointment.");
        if (!"canceled".equalsIgnoreCase(a.getStatus())) throw new IllegalArgumentException("Only canceled appointments can be deleted.");
        // delete lines then appointment
        appointmentServiceLineRepository.deleteAllByAppointment(a);
        appointmentRepository.delete(a);
    }
}
