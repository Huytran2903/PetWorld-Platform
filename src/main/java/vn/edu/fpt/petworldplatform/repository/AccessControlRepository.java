package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import vn.edu.fpt.petworldplatform.entity.AccessControl;

import java.util.List;

public interface AccessControlRepository extends JpaRepository<AccessControl, Integer> {
    List<AccessControl> findByRoleId(Integer roleId);

    @Modifying
    void deleteByRoleId(Integer roleId);
}
