package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.dto.PetStatisticsDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.PetVaccinations;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.ServiceItem;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.PetVaccinationRepository;
import vn.edu.fpt.petworldplatform.repository.ServiceItemRepository;
import vn.edu.fpt.petworldplatform.util.FileUploadUtil;

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

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    //OanhTP
    public Page<Pets> findAllPets(Pageable pageable) {
        return petRepo.findAll(pageable);
    }

    // --- 1. Lấy danh sách - Customer
    //OanhTP
    public Page<Pets> getAllPetWithPagination(Pageable pageable) {
        return petRepo.findByOwnerIsNullAndPriceIsNotNull(pageable);
    }

    public Page<Pets> getAllPet(Pageable pageable) {
        return petRepo.findAll(pageable);
    }

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

    public List<ServiceItem> getAllVaccines() {
        return serviceItemRepository.findByServiceTypeIdAndIsActiveTrue(1);
    }

    public Page<Pets> findPetByNameAndType(String keyword, String type, Pageable pageable) {
        // Đảm bảo keyword không bị null để tránh lỗi SQL
        String searchName = (keyword != null) ? keyword.trim() : "";
        String searchType = (type != null) ? type.trim() : "";

        return petRepo.findAllByNameContainingIgnoreCaseAndPetTypeIgnoreCaseAndOwnerIsNullAndPriceIsNotNull(
                searchName, searchType, pageable
        );
    }

    public Page<Pets> searchPetByName(String keyword, Pageable pageable) {
        return petRepo.findAllByNameContainingIgnoreCase(keyword, pageable);
    }

    //Filter theo status Pet
    public Page<Pets> getPetByOwnerIsNull(String status, Pageable pageable) {
        return petRepo.findAllByOwnerIsNull(status, pageable);
    }

    public Page<Pets> getPetByOwnerNotNull(String status, Pageable pageable) {
        return petRepo.findAllByOwnerIsNotNull(status, pageable);
    }

    public Page<Pets> getAvailablePetsByType(String type, Pageable pageable) {

        // Nếu không truyền type, hoặc chọn "All" -> Lấy tất cả pet đang bán
        if (type == null || type.trim().isEmpty() || type.equalsIgnoreCase("All")) {
            return petRepo.findAllByOwnerIsNullAndPriceIsNotNull(pageable);
        }

        // Nếu có chọn Type cụ thể ("Dog", "Cat", "Bird"...) -> Lọc theo loại và đang bán
        return petRepo.findAllByPetTypeIgnoreCase(type, pageable);
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

        boolean isOtherSpecies = "Other".equalsIgnoreCase(species);

        // Service pets
        List<Object[]> serviceResults = isOtherSpecies
                ? petRepo.countServicePetsByOtherSpeciesAndDateRange(startDateTime, endDateTime)
                : petRepo.countServicePetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long serviceCount = serviceResults.isEmpty() ? 0 : ((Number) serviceResults.get(0)[1]).longValue();

        // Sale pets
        List<Object[]> saleResults = isOtherSpecies
                ? petRepo.countSalePetsByOtherSpeciesAndDateRange(startDateTime, endDateTime)
                : petRepo.countSalePetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long saleCount = saleResults.isEmpty() ? 0 : ((Number) saleResults.get(0)[1]).longValue();

        // Sold pets
        List<Object[]> soldResults = isOtherSpecies
                ? petRepo.countSoldPetsByOtherSpeciesAndDateRange(startDateTime, endDateTime)
                : petRepo.countSoldPetsBySpeciesAndDateRange(species, startDateTime, endDateTime);
        long soldCount = soldResults.isEmpty() ? 0 : ((Number) soldResults.get(0)[1]).longValue();

        long total = serviceCount + saleCount + soldCount;

        // Keep a fixed category order so view lookups by index remain correct.
        stats.add(new PetStatisticsDTO.PetSpeciesStats("SERVICE", serviceCount,
                total > 0 ? (serviceCount * 100.0 / total) : 0.0));
        stats.add(new PetStatisticsDTO.PetSpeciesStats("SALE", saleCount,
                total > 0 ? (saleCount * 100.0 / total) : 0.0));
        stats.add(new PetStatisticsDTO.PetSpeciesStats("SOLD", soldCount,
                total > 0 ? (soldCount * 100.0 / total) : 0.0));

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
