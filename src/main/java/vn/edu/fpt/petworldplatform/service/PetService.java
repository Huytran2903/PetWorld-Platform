package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.util.FileUploadUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PetService {

    @Autowired
    private PetRepo petRepo;

    @Autowired
    private CustomerRepo customerRepo;

    private static final String UPLOAD_DIR =
            "src/main/resources/static/images/uploads/";


    //OanhTP
    public List<Pets> findAllPets() {
        return petRepo.findAll();
    }

    // --- 1. Lấy danh sách ---
    public List<Pets> getAllPets() {
        return petRepo.findAll();
    }

    public void savePet(Pets pet) {
        // Admin/shop pet: owner = null => bắt buộc có giá hợp lệ
        if (pet.getOwner() == null) {
            if (pet.getPrice() == null || pet.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá bán là bắt buộc và phải lớn hơn 0 cho thú cưng của shop.");
            }
            pet.setIsAvailable(true);
        } else {
            // Pet của customer không cần giá
            pet.setPrice(null);
            if (pet.getIsAvailable() == null) {
                pet.setIsAvailable(false);
            }
        }

        petRepo.save(pet);
    }

    public Pets getPetById(Integer id) {
        return petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("No pet found with this ID: " + id));
    }

    public void removePet(Integer id) {
        petRepo.deleteById(id);
    }

    public void createPet(PetCreateDTO dto) throws IOException {
        Pets pet = new Pets();

        pet.setName(normalizeText(dto.getName()));
        pet.setPetType(normalizeText(dto.getSpecies()));
        pet.setBreed(normalizeText(dto.getBreed()));
        pet.setAgeMonths(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setImageUrl(dto.getImageUrl());

        pet.setWeightKg(dto.getWeightKg());
        pet.setColor(normalizeText(dto.getColor()));
        pet.setGender(dto.getGender());
        pet.setNote(dto.getNote());

        MultipartFile file = dto.getImageFile();
        if (file != null && !file.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            String uploadDir = "src/main/resources/static/images/";

            FileUploadUtil.saveFile(uploadDir, fileName, file);

            pet.setImageUrl("/images/" + fileName);
        }

        if ("shop".equalsIgnoreCase(dto.getCreatePetOwnerType())) {
            // Pet của shop: bắt buộc phải có giá hợp lệ
            if (dto.getPrice() == null || dto.getPrice() <= 0) {
                throw new IllegalArgumentException("Giá bán là bắt buộc và phải lớn hơn 0 cho thú cưng của shop.");
            }
            pet.setPrice(BigDecimal.valueOf(dto.getPrice()));
            pet.setOwner(null);
            pet.setIsAvailable(true);

        } else {
            if (dto.getOwnerId() == null) {
                throw new IllegalArgumentException("Vui lòng nhập ID Khách hàng (Owner ID)!");
            }

            Customer owner = customerRepo.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng có ID: " + dto.getOwnerId()));

            pet.setOwner(owner);
            pet.setPrice(null);
            pet.setIsAvailable(false);
        }

        petRepo.save(pet);
    }

    public void updatePet(Pets petFromForm) {
        Pets existingPet = getPetById(petFromForm.getPetID());

        existingPet.setName(petFromForm.getName());
        existingPet.setPetType(petFromForm.getPetType());
        existingPet.setBreed(petFromForm.getBreed());
        existingPet.setAgeMonths(petFromForm.getAgeMonths());
        existingPet.setImageUrl(petFromForm.getImageUrl());
        existingPet.setDescription(petFromForm.getDescription());
        existingPet.setIsAvailable(petFromForm.getIsAvailable());

        if (existingPet.getOwner() == null) {
            existingPet.setPrice(petFromForm.getPrice());
        }

        petRepo.save(existingPet);
    }

    public long getTotalPets() {
        return petRepo.countTotalPets();
    }

    public List<Object[]> getPetStatsBySpecies() {
        return petRepo.countPetsBySpecies();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return normalized;
        }

        char first = normalized.charAt(0);
        if (Character.isLetter(first)) {
            normalized = Character.toUpperCase(first) + normalized.substring(1);
        }

        return normalized;
    }

}
