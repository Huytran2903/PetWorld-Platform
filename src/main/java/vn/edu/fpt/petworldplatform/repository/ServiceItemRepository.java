package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Integer> {

    List<ServiceItem> findAllByOrderByServiceTypeAscNameAsc();

    List<ServiceItem> findByServiceTypeOrderByNameAsc(String serviceType);

    List<ServiceItem> findByServiceTypeIgnoreCaseOrderByNameAsc(String serviceType);

    boolean existsByNameIgnoreCaseAndServiceType(String name, String serviceType);

    boolean existsByNameIgnoreCaseAndServiceTypeAndIdNot(String name, String serviceType, Integer excludeId);

    /** Count appointments that use this service (for soft-delete constraint). */
    @Query(value = "SELECT COUNT(*) FROM AppointmentServices WHERE ServiceID = :id", nativeQuery = true)
    long countAppointmentsByServiceId(@Param("id") Integer serviceId);
}
