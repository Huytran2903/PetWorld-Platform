package vn.edu.fpt.petworldplatform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    // Đường dẫn thư mục lưu file (Nên cấu hình trong application.properties)
    // "." nghĩa là thư mục gốc của dự án khi chạy
    private static final String UPLOAD_DIR = "./uploads";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. Kiểm tra file rỗng
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File không được để trống");
            }

            // 2. Tạo thư mục nếu chưa tồn tại
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 3. Xử lý tên file (Tránh trùng tên bằng UUID)
            // Lấy tên gốc
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            // Tạo tên mới: uuid_tengoc.jpg
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // 4. Lưu file
            try (InputStream inputStream = file.getInputStream()) {
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

                // Trả về đường dẫn hoặc tên file để lưu vào Database
                return ResponseEntity.ok("File đã lưu thành công: " + fileName);
            }

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi upload file: " + e.getMessage());
        }
    }
}
