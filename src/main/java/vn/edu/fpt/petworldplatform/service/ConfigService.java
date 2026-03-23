package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;
import vn.edu.fpt.petworldplatform.repository.SystemConfigsRepository;
import vn.edu.fpt.petworldplatform.util.SecuritySupport; // Import class xịn sò của bạn vào

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConfigService {

    @Autowired
    private SystemConfigsRepository configRepo;

    @Autowired
    private SecuritySupport securitySupport;

    public List<SystemConfigs> getAllConfigs() {
        return configRepo.findAll();
    }

    @Transactional
    public void updateConfigs(List<SystemConfigs> configs) {

        Staff currentStaff = securitySupport.getCurrentAuthenticatedStaff();

        for (SystemConfigs config : configs) {
            config.setUpdatedByStaff(currentStaff);
            config.setUpdatedAt(LocalDateTime.now());
        }

        configRepo.saveAll(configs);
    }

    @Transactional
    public void saveSingleConfig(SystemConfigs config) {
        Staff currentStaff = securitySupport.getCurrentAuthenticatedStaff();
        config.setUpdatedByStaff(currentStaff);
        config.setUpdatedAt(LocalDateTime.now());

        configRepo.save(config);
    }
}