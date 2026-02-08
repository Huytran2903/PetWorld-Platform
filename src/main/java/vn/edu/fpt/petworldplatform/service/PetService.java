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

    public List<Pets> getAllPets() {
        return petRepo.findAll();
    }

    private static final String UPLOAD_DIR =
            "src/main/resources/static/images/uploads/";


    //OanhTP
    public List<Pets> getAllPets2() {
        return petRepository.findAll();
    }

    // --- 1. Lấy danh sách ---
    public List<Pets> getAllPets() {
        return petRepo.findAll();
    }

    public void savePet(Pets pet) {
        petRepo.save(pet);
    }

    public Pets getPetById(Long id) {
        return petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng ID: " + id));
    }

    public void createPet(PetCreateDTO dto) throws IOException {
        Pets pet = new Pets();

        pet.setName(dto.getName());
        pet.setPetType(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setAgeMonths(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setImageUrl(dto.getImageUrl());

        pet.setWeightKg(dto.getWeightKg());
        pet.setColor(dto.getColor());
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
            // Pet của shop
            if (dto.getPrice() != null) {
                pet.setPrice(BigDecimal.valueOf(dto.getPrice()));
            } else {
                pet.setPrice(BigDecimal.ZERO);
            }
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



}
