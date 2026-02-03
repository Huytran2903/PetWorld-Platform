package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.AppointmentServiceLine;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.AppointmentRepository;
import vn.edu.fpt.petworldplatform.repository.AppointmentServiceLineRepository;
import vn.edu.fpt.petworldplatform.repository.PetRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int LEAD_TIME_HOURS = 2;
    private static final LocalTime OPEN_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(20, 0);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentServiceLineRepository appointmentServiceLineRepository;
    private final PetRepository petRepository;
    private final ServiceItemRepository serviceItemRepository;

    public List<Pets> findPetsByCustomerId(Long customerId) {
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
     * Use case: Selected time outside operating hours (08:00 - 20:00).
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

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger sequence = new AtomicInteger(1);

    private String generateAppointmentCode() {
        String datePart = LocalDate.now().format(CODE_DATE);
        int seq = sequence.getAndIncrement();
        if (seq > 9999) sequence.set(1);
        return "APT-" + datePart + "-" + String.format("%04d", seq);
    }
}
