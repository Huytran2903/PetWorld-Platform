package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Staff;

import java.util.Optional;

@Repository
public interface StaffRepo extends JpaRepository<Staff, Integer> {
    Optional<Staff> findByUsername(String username);

    Optional<Staff> findByEmail(String email);
}
