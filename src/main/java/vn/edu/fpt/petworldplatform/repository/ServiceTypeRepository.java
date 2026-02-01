package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.ServiceType;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, Integer> {

    List<ServiceType> findAllByOrderByNameAsc();

    Optional<ServiceType> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    /** Count services (dbo.Services) that use this type name - for BR-21 delete constraint. */
    @Query(value = "SELECT COUNT(*) FROM Services WHERE LOWER(ServiceType) = LOWER(:name)", nativeQuery = true)
    long countServicesByTypeName(@Param("name") String name);
}
