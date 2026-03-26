package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Integer> {

    Optional<Staff> findByUsername(String username);

    Optional<Staff> findByUsernameIgnoreCase(String username);

    Optional<Staff> findByEmail(String email);

    Optional<Staff> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    List<Staff> findByIsActiveTrue();

    @Query("SELECT s FROM Staff s WHERE " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Staff> searchStaffs(@Param("keyword") String keyword, Pageable pageable);

    boolean existsByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndStaffIdNot(String username, Integer staffId);

    boolean existsByPhoneAndStaffIdNot(String phone, Integer staffId);
}
