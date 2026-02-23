package vn.edu.fpt.petworldplatform.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Role;

import java.util.Optional;

@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
    Optional<Role> findById(@NotNull(message = "Please select a role") Integer roleId);
}
