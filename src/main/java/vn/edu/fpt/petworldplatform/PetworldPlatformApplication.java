package vn.edu.fpt.petworldplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "vn.edu.fpt.petworldplatform.repository")

public class PetworldPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetworldPlatformApplication.class, args);
    }

}
