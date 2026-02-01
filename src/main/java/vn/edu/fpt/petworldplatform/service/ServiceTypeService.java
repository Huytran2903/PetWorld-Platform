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

    /** BR-21: Cannot delete if has associated services or bookings. */
    public long countServicesUsingTypeName(String name) {
        return serviceTypeRepository.countServicesByTypeName(name);
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

    /** Soft delete: set IsActive = false. BR-21: block if has associated services/bookings. */
    @Transactional
    public boolean softDelete(Integer id) {
        Optional<ServiceType> opt = serviceTypeRepository.findById(id);
        if (opt.isEmpty()) return false;
        ServiceType st = opt.get();
        long used = countServicesUsingTypeName(st.getName());
        if (used > 0) return false; // BR-21: cannot delete
        st.setIsActive(false);
        serviceTypeRepository.save(st);
        return true;
    }
}
