package vn.edu.fpt.petworldplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Đối với ảnh trong resource/static:
        // Spring Boot TỰ ĐỘNG xử lý, bạn KHÔNG CẦN dòng code nào cho nó cả.
        // Mặc định truy cập: /images/logo.png -> trỏ vào static/images/logo.png

        // 2. Đối với ảnh Upload (Ổ D):
        // Chúng ta sẽ dùng một đường dẫn ảo khác, ví dụ "/pet-images/**"
        // để tránh trùng với thư mục "images" gốc của web.

        Path uploadDir = Paths.get("D:/SPRING26/HSF302_01/img");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        registry.addResourceHandler("/pet-images/**") // Đường dẫn ảo trên trình duyệt
                .addResourceLocations("file:/" + uploadPath + "/"); // Đường dẫn thật trên ổ cứng
    }
}
