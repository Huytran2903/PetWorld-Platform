package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;
import vn.edu.fpt.petworldplatform.repository.SystemConfigsRepository;

import java.util.List;

@Service
public class ConfigService {

    @Autowired
    private SystemConfigsRepository configRepo;

    public List<SystemConfigs> getAllConfigs() {
        return configRepo.findAll();
    }

    @Transactional
    public void updateConfigs(List<SystemConfigs> configs) {
        configRepo.saveAll(configs);
    }

    @Transactional
    public void saveSingleConfig(SystemConfigs config) {
        configRepo.save(config);
    }
}
