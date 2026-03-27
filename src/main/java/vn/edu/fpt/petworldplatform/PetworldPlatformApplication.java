package vn.edu.fpt.petworldplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class PetworldPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetworldPlatformApplication.class, args);
    }

}
