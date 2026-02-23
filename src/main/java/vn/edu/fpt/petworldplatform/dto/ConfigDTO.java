package vn.edu.fpt.petworldplatform.dto;

import lombok.Getter;
import lombok.Setter;
import vn.edu.fpt.petworldplatform.entity.SystemConfigs;

import java.util.List;

@Getter
@Setter
public class ConfigDTO {
    private List<SystemConfigs> configs;
}
