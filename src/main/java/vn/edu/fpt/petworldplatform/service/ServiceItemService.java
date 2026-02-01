package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceItemService {

    private final ServiceItemRepository serviceItemRepository;

    public List<ServiceItem> findAll() {
        return serviceItemRepository.findAllByOrderByServiceTypeAscNameAsc();
    }

    public List<ServiceItem> findByServiceType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return findAll();
        }
        return serviceItemRepository.findByServiceTypeOrderByNameAsc(serviceType.trim());
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
        return serviceItemRepository.save(entity);
    }

    /** Soft delete: set IsActive = false. Block if has associated appointments (BR-21). */
    @Transactional
    public boolean softDelete(Integer id) {
        Optional<ServiceItem> opt = serviceItemRepository.findById(id);
        if (opt.isEmpty()) return false;
        long used = serviceItemRepository.countAppointmentsByServiceId(id);
        if (used > 0) return false;
        ServiceItem s = opt.get();
        s.setIsActive(false);
        serviceItemRepository.save(s);
        return true;
    }
}
