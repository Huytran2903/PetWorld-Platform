package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.Role;

@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
}
