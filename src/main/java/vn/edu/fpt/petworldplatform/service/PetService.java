package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pet;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepository;

import java.util.List;

@Service
public class PetService {

    @Autowired
    private PetRepository petRepo;

    @Autowired
    private PetRepo petRepository;

    @Autowired
    private CustomerRepo customerRepo;

    //OanhTP

    // --- 1. Lấy danh sách ---
    public List<Pet> getAllPets() {
        return petRepo.findAll();
    }

    // --- 2. Lấy chi tiết (Dùng cho Detail & Update) ---
    public Pet getPetById(Integer id) {
        return petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng với ID: " + id));
    }

    //OanhTP
    public void savePet(Pets pet) {
        petRepository.save(pet);
    }

    public Pets getPetById(Long id) {
        return petRepository.findById(id).get();
    }


    // --- 3. Tạo mới (Create) ---
    public void createPet(PetCreateDTO dto) {
        Pet pet = new Pet();

        // Map dữ liệu từ Form sang Entity
        pet.setName(dto.getName());
        pet.setPetType(dto.getSpecies()); // Form gửi "dog"/"cat"
        pet.setBreed(dto.getBreed());
        pet.setAgeMonths(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setImageUrl(dto.getImageUrl());

        // Logic phân loại: Shop vs Customer
        if ("shop".equals(dto.getCreatePetOwnerType())) {
            pet.setPrice(dto.getPrice());
            pet.setOwner(null);
            pet.setAvailable(true);
        } else {
            // Tìm chủ nhân theo ID (Long)
            Customer owner = customerRepo.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng ID: " + dto.getOwnerId()));

            pet.setOwner(owner);
            pet.setPrice(null);
            pet.setAvailable(false);
        }

        petRepo.save(pet);
    }

    // --- 4. Cập nhật (Update) - MỚI THÊM ---
    public void updatePet(Pet petFromForm) {
        // Bước 1: Lấy dữ liệu gốc từ DB lên để đảm bảo không bị mất thông tin quan trọng (như Owner)
        Pet existingPet = getPetById(petFromForm.getId());

        // Bước 2: Chỉ cập nhật những trường cho phép sửa
        existingPet.setName(petFromForm.getName());
        existingPet.setPetType(petFromForm.getPetType());
        existingPet.setBreed(petFromForm.getBreed());
        existingPet.setAgeMonths(petFromForm.getAgeMonths());
        existingPet.setImageUrl(petFromForm.getImageUrl());
        existingPet.setDescription(petFromForm.getDescription());
        existingPet.setAvailable(petFromForm.isAvailable()); // Cập nhật trạng thái

        // Bước 3: Logic giá tiền
        // Nếu là Pet của Shop (không có chủ) thì mới cho sửa giá
        if (existingPet.getOwner() == null) {
            existingPet.setPrice(petFromForm.getPrice());
        }
        // Lưu ý: Không cập nhật setOwner() để tránh việc form gửi null làm mất chủ nhân

        // Bước 4: Lưu xuống DB
        petRepo.save(existingPet);
    }
}