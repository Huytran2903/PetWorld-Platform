package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.ServiceType;
import vn.edu.fpt.petworldplatform.repository.ServiceTypeRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceTypeService {

    private final ServiceTypeRepository serviceTypeRepository;

    public List<ServiceType> findAll() {
        return serviceTypeRepository.findAllByOrderByNameAsc();
    }

    public Optional<ServiceType> findById(Integer id) {
        return serviceTypeRepository.findById(id);
    }

    /** Count services that use this type name. */
    public long countServicesUsingTypeName(String name) {
        return serviceTypeRepository.countServicesByTypeName(name);
    }

    /** Count appointments that are using services of this type. */
    public long countAppointmentsUsingTypeName(String name) {
        return serviceTypeRepository.countAppointmentsByServiceTypeName(name);
    }

    /** Validate name unique (BR-30 / name uniqueness). */
    public boolean isNameDuplicate(String name, Integer excludeId) {
        if (excludeId == null) {
            return serviceTypeRepository.existsByNameIgnoreCase(name);
        }
        return serviceTypeRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Transactional
    public ServiceType save(ServiceType entity) {
        return serviceTypeRepository.save(entity);
    }

    /**
     * Delete behavior:
     * - If service type is NOT used by any service or appointment: hard delete.
     * - If it IS used: set isActive = false (soft delete) and return deactivated result.
     */
    @Transactional
    public DeleteResult deleteOrDeactivate(Integer id) {
        Optional<ServiceType> opt = serviceTypeRepository.findById(id);
        if (opt.isEmpty()) {
            return DeleteResult.notFound();
        }

        ServiceType st = opt.get();
        String typeName = st.getName();

        long usedServices = countServicesUsingTypeName(typeName);
        long usedAppointments = countAppointmentsUsingTypeName(typeName);

        if (usedServices > 0 || usedAppointments > 0) {
            // In use -> Deactivate
            st.setIsActive(false);
            serviceTypeRepository.save(st);
            return DeleteResult.successDeactivatedBecauseInUse(usedServices, usedAppointments);
        }

        // Not in use -> Hard delete
        try {
            serviceTypeRepository.delete(st);
            serviceTypeRepository.flush();
            return DeleteResult.successDeleted();
        } catch (RuntimeException ex) {
            // Fallback to deactivate if unexpected constraint occurs
            st.setIsActive(false);
            serviceTypeRepository.save(st);
            return DeleteResult.successDeactivatedBecauseInUse(usedServices, usedAppointments);
        }
    }

    public static final class DeleteResult {
        private final boolean ok;
        private final boolean existed;
        private final boolean deleted;
        private final boolean deactivated;
        private final long usedServices;
        private final long usedAppointments;

        private DeleteResult(boolean ok, boolean existed, boolean deleted, boolean deactivated, long usedServices, long usedAppointments) {
            this.ok = ok;
            this.existed = existed;
            this.deleted = deleted;
            this.deactivated = deactivated;
            this.usedServices = usedServices;
            this.usedAppointments = usedAppointments;
        }

        public static DeleteResult notFound() {
            return new DeleteResult(false, false, false, false, 0, 0);
        }

        public static DeleteResult successDeleted() {
            return new DeleteResult(true, true, true, false, 0, 0);
        }

        public static DeleteResult successDeactivatedBecauseInUse(long usedServices, long usedAppointments) {
            return new DeleteResult(true, true, false, true, usedServices, usedAppointments);
        }

        public boolean isOk() { return ok; }
        public boolean isExisted() { return existed; }
        public boolean isDeleted() { return deleted; }
        public boolean isDeactivated() { return deactivated; }
        public long getUsedServices() { return usedServices; }
        public long getUsedAppointments() { return usedAppointments; }
    }
}
