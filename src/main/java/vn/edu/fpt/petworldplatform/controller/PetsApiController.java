package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.Staff;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.service.PetService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/pets")
public class PetsApiController {
    @Autowired
    private PetRepo petRepo;

    @Autowired
    private PetService petService;

    // ==========================================
    // 1. API THÊM MỚI PET (CREATE)
    // ==========================================
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<Object> addPet(
            @Validated @ModelAttribute Pets pet,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        // 1. Kiểm tra lỗi validate
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getValidationErrors(result));
        }

        try {
            // 2. Xử lý danh sách tiêm phòng
            if (pet.getVaccinations() == null) {
                pet.setVaccinations(new ArrayList<>());
            }

            if (Boolean.TRUE.equals(pet.getIsVaccinated())) {
                PetVaccinations newVaccine = new PetVaccinations();
                newVaccine.setPet(pet);
                newVaccine.setVaccineName(pet.getVaccineName());
                newVaccine.setNote(pet.getVaccineNote());
                if (pet.getNextDueDate() != null) {
                    newVaccine.setNextDueDate(pet.getNextDueDate());
                }
                newVaccine.setAdministeredDate(LocalDate.now());

                if (pet.getVaccinationStaffID() != null) {
                    Staff performedBy = new Staff();
                    performedBy.setStaffId(pet.getVaccinationStaffID());
                    newVaccine.setPerformedByStaff(performedBy);
                }
                pet.getVaccinations().add(newVaccine);
            }

            // 3. Xử lý lưu ảnh
            pet.setImageUrl(saveImageLocally(imageFile));

            // 4. Lưu vào Database
            petService.savePet(pet);

            // 5. Trả về kết quả JSON
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pet created successfully");
            response.put("data", pet);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating pet: " + e.getMessage()));
        }
    }

    // ==========================================
    // 2. API CẬP NHẬT PET (UPDATE)
    // ==========================================
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<Object> updatePet(
            @PathVariable("id") Integer id,
            @Validated @ModelAttribute Pets petDetails,
            BindingResult result,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(getValidationErrors(result));
        }

        try {
            // 1. Kiểm tra Pet có tồn tại không
            Pets existingPet = petService.getPetById(id);
            if (existingPet == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Pet with ID " + id + " not found"));
            }

            // 2. Cập nhật ID để ghi đè (Update)
            petDetails.setPetID(id);

            // 3. Ráp lại danh sách tiêm phòng cũ
            petDetails.setVaccinations(existingPet.getVaccinations());

            // Xử lý logic tiêm phòng mới nếu có update
            if (Boolean.TRUE.equals(petDetails.getIsVaccinated())) {
                if (petDetails.getVaccinations().isEmpty()) {
                    PetVaccinations newVaccine = new PetVaccinations();
                    newVaccine.setPet(petDetails);
                    newVaccine.setVaccineName(petDetails.getVaccineName());
                    newVaccine.setNote(petDetails.getVaccineNote());
                    if (petDetails.getNextDueDate() != null) newVaccine.setNextDueDate(petDetails.getNextDueDate());
                    newVaccine.setAdministeredDate(LocalDate.now());

                    if (petDetails.getVaccinationStaffID() != null) {
                        Staff performedBy = new Staff();
                        performedBy.setStaffId(petDetails.getVaccinationStaffID());
                        newVaccine.setPerformedByStaff(performedBy);
                    }
                    petDetails.getVaccinations().add(newVaccine);
                }
            } else {
                if (petDetails.getVaccinations() != null) {
                    petDetails.getVaccinations().clear();
                }
            }

            // 4. Xử lý ảnh (Nếu không có ảnh mới thì lấy ảnh cũ)
            String newImageUrl = saveImageLocally(imageFile);
            if (newImageUrl != null) {
                petDetails.setImageUrl(newImageUrl);
            } else {
                petDetails.setImageUrl(existingPet.getImageUrl());
            }

            // 5. Lưu vào Database
             petService.savePet(petDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Pet updated successfully");
            response.put("data", petDetails);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating pet: " + e.getMessage()));
        }
    }

    // ==========================================
    // CÁC HÀM HỖ TRỢ (HELPER METHODS)
    // ==========================================

    private String saveImageLocally(MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileName = UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();
            try (InputStream inputStream = imageFile.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + fileName;
        }
        return null;
    }

    // Hàm Gom lỗi validation thành Map chuẩn JSON
    private Map<String, String> getValidationErrors(BindingResult result) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : result.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    @GetMapping("")
    public List<Pets> getAllPets() {
        return petRepo.findAll();
    }
}
