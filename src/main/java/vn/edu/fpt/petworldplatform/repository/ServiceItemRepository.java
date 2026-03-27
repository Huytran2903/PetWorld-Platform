package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;

import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Integer> {

    /**
     * Sort by service type name, then service name (derived query name would misparse nested _Name + Asc).
     */
    @Query("SELECT s FROM ServiceItem s ORDER BY s.serviceType.name ASC, s.name ASC")
    List<ServiceItem> findAllOrderedByTypeAndName();

    @Query("SELECT s FROM ServiceItem s WHERE LOWER(s.serviceType.name) = LOWER(:typeName) ORDER BY s.name ASC")
    List<ServiceItem> findByServiceTypeNameIgnoreCaseOrderByNameAsc(@Param("typeName") String typeName);

    @Query("SELECT COUNT(s) FROM ServiceItem s WHERE LOWER(s.name) = LOWER(:name) AND LOWER(s.serviceType.name) = LOWER(:typeName)")
    long countByNameIgnoreCaseAndServiceTypeName(@Param("name") String name, @Param("typeName") String typeName);

    @Query("SELECT COUNT(s) FROM ServiceItem s WHERE LOWER(s.name) = LOWER(:name) AND LOWER(s.serviceType.name) = LOWER(:typeName) AND s.id <> :excludeId")
    long countByNameIgnoreCaseAndServiceTypeNameAndIdNot(
            @Param("name") String name, @Param("typeName") String typeName, @Param("excludeId") Integer excludeId);

    /**
     * Count appointments that use this service (for soft-delete constraint).
     */
    @Query(value = "SELECT COUNT(*) FROM AppointmentServices WHERE ServiceID = :id", nativeQuery = true)
    long countAppointmentsByServiceId(@Param("id") Integer serviceId);


    List<ServiceItem> findByServiceTypeIdAndIsActiveTrue(Integer serviceTypeId);
}
