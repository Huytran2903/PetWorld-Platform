package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;
    private final PetVaccinationRepository petVaccinationRepository;

    public List<ServiceItem> findAll() {
        return serviceItemRepository.findAllByOrderByServiceTypeAscNameAsc();
    }

    public List<ServiceItem> findByServiceType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return findAll();
        }
        return serviceItemRepository.findByServiceTypeIgnoreCaseOrderByNameAsc(serviceType.trim());
    }

    public Optional<ServiceItem> findById(Integer id) {
        return serviceItemRepository.findById(id);
    }

    /** Duplicate = same name within same service type (BR-29/BR-30). */
    public boolean isNameDuplicate(String name, String serviceType, Integer excludeId) {
        if (name == null || serviceType == null) return false;
        String n = name.trim();
        String t = serviceType.trim();
        if (excludeId == null) {
            return serviceItemRepository.existsByNameIgnoreCaseAndServiceType(n, t);
        }
        return serviceItemRepository.existsByNameIgnoreCaseAndServiceTypeAndIdNot(n, t, excludeId);
    }

    @Transactional
    public ServiceItem save(ServiceItem entity) {
        if (entity == null) throw new IllegalArgumentException("Service is required.");

        Integer id = entity.getId();
        if (id != null) {
            Optional<ServiceItem> existingOpt = serviceItemRepository.findById(id);
            if (existingOpt.isPresent()) {
                ServiceItem existing = existingOpt.get();
                boolean existingIsVaccine = existing.getServiceType() != null
                        && existing.getServiceType().trim().equalsIgnoreCase("vaccination");
                boolean newIsVaccine = entity.getServiceType() != null
                        && entity.getServiceType().trim().equalsIgnoreCase("vaccination");

                // Prevent retroactive behavior changes for vaccine services that already have usage/records.
                if (existingIsVaccine && newIsVaccine) {
                    Boolean oldMode = existing.getIsOneTimeVaccine();
                    Boolean newMode = entity.getIsOneTimeVaccine();
                    boolean modeChanged = (oldMode == null ? Boolean.FALSE : oldMode) != (newMode == null ? Boolean.FALSE : newMode);

                    if (modeChanged) {
                        long usedAppointments = serviceItemRepository.countAppointmentsByServiceId(id);
                        long vaccinationRecords = 0;
                        if (existing.getName() != null && !existing.getName().isBlank()) {
                            vaccinationRecords = petVaccinationRepository.countByVaccineNameIgnoreCase(existing.getName().trim());
                        }
                        if (usedAppointments > 0 || vaccinationRecords > 0) {
                            throw new IllegalStateException(
                                    "Cannot change the vaccine schedule mode (one-time vs. due-date) because this service already has existing vaccination/appointment data."
                            );
                        }
                    }
                }
            }
        }
        return serviceItemRepository.save(entity);
    }

    /**
     * Delete behavior:
     * - If service is NOT used by any appointment: hard delete.
     * - If it IS used: set isActive = false (soft delete) and return deactivated result.
     */
    @Transactional
    public DeleteResult deleteOrDeactivate(Integer id) {
        Optional<ServiceItem> opt = serviceItemRepository.findById(id);
        if (opt.isEmpty()) {
            return DeleteResult.notFound();
        }

        ServiceItem s = opt.get();

        long usedAppointments = serviceItemRepository.countAppointmentsByServiceId(id);
        if (usedAppointments > 0) {
            s.setIsActive(false);
            serviceItemRepository.save(s);
            return DeleteResult.successDeactivatedBecauseInUse(usedAppointments);
        }

        // Not in use -> Hard delete
        try {
            serviceItemRepository.delete(s);
            serviceItemRepository.flush();
            return DeleteResult.successDeleted();
        } catch (RuntimeException ex) {
            // Fallback to deactivate if unexpected constraint occurs
            s.setIsActive(false);
            serviceItemRepository.save(s);
            return DeleteResult.successDeactivatedBecauseInUse(usedAppointments);
        }
    }

    public static final class DeleteResult {
        private final boolean ok;
        private final boolean existed;
        private final boolean deleted;
        private final boolean deactivated;
        private final long usedAppointments;

        private DeleteResult(boolean ok, boolean existed, boolean deleted, boolean deactivated, long usedAppointments) {
            this.ok = ok;
            this.existed = existed;
            this.deleted = deleted;
            this.deactivated = deactivated;
            this.usedAppointments = usedAppointments;
        }

        public static DeleteResult notFound() {
            return new DeleteResult(false, false, false, false, 0);
        }

        public static DeleteResult successDeleted() {
            return new DeleteResult(true, true, true, false, 0);
        }

        public static DeleteResult successDeactivatedBecauseInUse(long usedAppointments) {
            return new DeleteResult(true, true, false, true, usedAppointments);
        }

        public boolean isOk() { return ok; }
        public boolean isExisted() { return existed; }
        public boolean isDeleted() { return deleted; }
        public boolean isDeactivated() { return deactivated; }
        public long getUsedAppointments() { return usedAppointments; }
    }
}
