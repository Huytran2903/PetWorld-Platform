package vn.edu.fpt.petworldplatform.repository;

import vn.edu.fpt.petworldplatform.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);

    Optional<Staff> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Staff> findByIsActiveTrue();
}
