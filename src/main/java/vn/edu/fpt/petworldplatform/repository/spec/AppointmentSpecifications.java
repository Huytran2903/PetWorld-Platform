package vn.edu.fpt.petworldplatform.repository.spec;

import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.petworldplatform.entity.Appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {}

    public static Specification<Appointment> hasStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return cb.conjunction();
            return cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase());
        };
    }

    public static Specification<Appointment> appointmentDateFrom(LocalDate from) {
        return (root, query, cb) -> {
            if (from == null) return cb.conjunction();
            LocalDateTime fromStart = from.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get("appointmentDate"), fromStart);
        };
    }

    public static Specification<Appointment> appointmentDateTo(LocalDate to) {
        return (root, query, cb) -> {
            if (to == null) return cb.conjunction();
            LocalDateTime toEnd = to.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get("appointmentDate"), toEnd);
        };
    }

    public static Specification<Appointment> keywordLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim().toLowerCase() + "%";
            // Current data model has only customerId/petId (no join to customer tables)
            // Search by appointmentCode / customerId / petId for now.
            return cb.or(
                    cb.like(cb.lower(root.get("appointmentCode")), like),
                    cb.like(cb.lower(root.get("customerId").as(String.class)), like),
                    cb.like(cb.lower(root.get("petId").as(String.class)), like)
            );
        };
    };
}
