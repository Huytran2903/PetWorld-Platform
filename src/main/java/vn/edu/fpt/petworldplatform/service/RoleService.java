package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Role;
import vn.edu.fpt.petworldplatform.repository.RoleRepo;

import java.util.List;

@Service
public class RoleService {
    @Autowired
    private RoleRepo roleRepo;

    public List<Role> getAllRoles(){
        return roleRepo.findAll();
    }

}

