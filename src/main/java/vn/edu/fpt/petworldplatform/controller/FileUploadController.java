package vn.edu.fpt.petworldplatform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

// 1. Dùng @RestController thay vì @Controller
// @RestController sẽ trả về dữ liệu (JSON/String) thay vì tìm file HTML
@RestController
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads";

    // Bỏ hàm getImageView vì bạn không cần trang HTML

    @PostMapping("/uploadFile")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {

        // Kiểm tra file rỗng
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload!");
        }

        try {
            // Tạo thư mục uploads
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Lưu file
            try (InputStream inputStream = file.getInputStream()) {
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }


            return ResponseEntity.ok("/uploads/" + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    // Xử lý lỗi quá dung lượng (Dùng @ExceptionHandler gọn hơn implement interface)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body("File size exceeds limit!");
    }
}