package vn.edu.fpt.petworldplatform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;
import vn.edu.fpt.petworldplatform.service.ConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalConfigAdvice {

    @Autowired
    private ConfigService configService;

    @ModelAttribute("globalConfigs")
    public Map<String, String> populateGlobalConfigs() {
        List<SystemConfigs> configsList = configService.getAllConfigs();

        Map<String, String> configMap = new HashMap<>();

        for (SystemConfigs config : configsList) {
            if (config.getConfigValue() != null) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
        }

        return configMap;
    }


}