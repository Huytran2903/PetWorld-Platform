package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.dto.PetStatisticsDTO;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

            String uploadDir = "uploads/";

            FileUploadUtil.saveFile(uploadDir, fileName, file);

            pet.setImageUrl("/uploads/" + fileName);
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

    public PetStatisticsDTO getPetStatistics(LocalDate startDate, LocalDate endDate) {
        PetStatisticsDTO stats = new PetStatisticsDTO();
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);

        // Convert LocalDate to LocalDateTime for database queries
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Overall counts
        stats.setTotalPets(petRepo.countPetsByDateRange(startDateTime, endDateTime));
        stats.setTotalServicePets(petRepo.countServicePetsByDateRange(startDateTime, endDateTime));
        stats.setTotalSalePets(petRepo.countSalePetsByDateRange(startDateTime, endDateTime));
        stats.setSoldPets(petRepo.countSoldPetsByDateRange(startDateTime, endDateTime));

        // For service completion, we'll use soldPets as completed services for now
        stats.setCompletedServicePets(stats.getSoldPets());

        // Species breakdown
        stats.setDogStats(getSpeciesStats("Dog", startDateTime, endDateTime));
        stats.setCatStats(getSpeciesStats("Cat", startDateTime, endDateTime));
        stats.setOtherStats(getSpeciesStats("Other", startDateTime, endDateTime));

        return stats;
    }

    private List<PetStatisticsDTO.PetSpeciesStats> getSpeciesStats(String species, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<PetStatisticsDTO.PetSpeciesStats> stats = new ArrayList<>();

        // Service pets
        List<Object[]> serviceResults = petRepo.countServicePetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long serviceCount = serviceResults.isEmpty() ? 0 : (Long) serviceResults.get(0)[1];

        // Sale pets
        List<Object[]> saleResults = petRepo.countSalePetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long saleCount = saleResults.isEmpty() ? 0 : (Long) saleResults.get(0)[1];

        // Sold pets
        List<Object[]> soldResults = petRepo.countSoldPetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long soldCount = soldResults.isEmpty() ? 0 : (Long) soldResults.get(0)[1];

        long total = serviceCount + saleCount;

        if (serviceCount > 0) {
            stats.add(new PetStatisticsDTO.PetSpeciesStats("SERVICE", serviceCount,
                    total > 0 ? (serviceCount * 100.0 / total) : 0.0));
        }

        if (saleCount > 0) {
            stats.add(new PetStatisticsDTO.PetSpeciesStats("SALE", saleCount,
                    total > 0 ? (saleCount * 100.0 / total) : 0.0));
        }

        if (soldCount > 0) {
            stats.add(new PetStatisticsDTO.PetSpeciesStats("SOLD", soldCount,
                    total > 0 ? (soldCount * 100.0 / total) : 0.0));
        }

        return stats;
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
