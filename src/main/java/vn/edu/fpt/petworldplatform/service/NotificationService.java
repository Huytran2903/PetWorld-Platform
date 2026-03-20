package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Appointment;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Notification;
import vn.edu.fpt.petworldplatform.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createForCustomer(Customer customer, Appointment appointment, String title, String message, String type) {
        if (customer == null) {
            return;
        }

        Notification notification = Notification.builder()
                .customer(customer)
                .appointment(appointment)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    public long getUnreadCount(Integer customerId) {
        if (customerId == null) {
            return 0;
        }
        return notificationRepository.countByCustomer_CustomerIdAndIsReadFalse(customerId);
    }

    public List<Notification> getLatest(Integer customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        return notificationRepository.findTop5ByCustomer_CustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Page<Notification> getPage(Integer customerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("createdAt").descending());
        return notificationRepository.findByCustomer_CustomerId(customerId, pageable);
    }

    @Transactional
    public Notification getAndMarkRead(Integer customerId, Integer notificationId) {
        Notification notification = notificationRepository.findByIdAndCustomer_CustomerId(notificationId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }

        return notification;
    }
}
