package vn.edu.fpt.petworldplatform.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.service.NotificationService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class VaccineDueNotificationJob {

    private static final String TYPE_VACCINE_DUE = "vaccine_due";

    private final PetVaccinationRepository petVaccinationRepository;
    private final NotificationService notificationService;

    /**
     * Nhắc vaccine theo đúng {@code nextDueDate} (chạy mỗi ngày).
     */
    @Scheduled(cron = "0 15 0 * * *")
    public void notifyVaccineDueToday() {
        LocalDate today = LocalDate.now();

        for (PetVaccinations v : petVaccinationRepository.findDueVaccinationsWithPetOwnerAndAppointment(today)) {
            if (v == null || v.getPet() == null || v.getPet().getOwner() == null || v.getAppointment() == null) {
                continue;
            }

            Integer customerId = v.getPet().getOwner().getCustomerId();
            Integer appointmentId = v.getAppointment().getId();

            // Tránh tạo trùng notification nếu job chạy nhiều lần / sau khi restart.
            if (notificationService.existsForCustomerAppointmentType(customerId, appointmentId, TYPE_VACCINE_DUE)) {
                continue;
            }

            String vaccineName = v.getVaccineName() != null ? v.getVaccineName().trim() : "-";
            String petName = v.getPet().getName() != null ? v.getPet().getName().trim() : "-";
            String dueDateStr = v.getNextDueDate() != null
                    ? v.getNextDueDate().format(DateTimeFormatter.ISO_DATE)
                    : today.format(DateTimeFormatter.ISO_DATE);

            String title = "Vaccination due";
            String message = "Vaccination \"" + vaccineName + "\" for pet \"" + petName + "\" is due (" + dueDateStr + "). "
                    + "Please schedule a visit to keep your pet protected.";

            notificationService.createForCustomer(v.getPet().getOwner(), v.getAppointment(), title, message, TYPE_VACCINE_DUE);
        }
    }
}

