package vn.edu.fpt.petworldplatform.service;// ... các import giữ nguyên

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pet;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepository;

import java.util.List;

@Service
public class PetService {

    @Autowired private PetRepository petRepo;
    @Autowired
    private CustomerRepo customerRepo;

    public List<Pet> getAllPets() {
        return petRepo.findAll();
    }

    public void createPet(PetCreateDTO dto) {
        Pet pet = new Pet();
        pet.setName(dto.getName());
        pet.setPetType(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setAgeMonths(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setImageUrl(dto.getImageUrl());

        // SỬA DÒNG 32: Gọi đúng tên getCreatePetOwnerType()
        if ("shop".equals(dto.getCreatePetOwnerType())) {

            // Pet của Shop
            pet.setPrice(dto.getPrice());
            pet.setOwner(null);
            pet.setAvailable(true);

        } else {
            // Pet của Khách
            // Dòng này sẽ hết lỗi đỏ vì ownerId bên DTO đã là Long
            Customer owner = customerRepo.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found ID: " + dto.getOwnerId()));

            pet.setOwner(owner);
            pet.setPrice(null);
            pet.setAvailable(false);
        }
        petRepo.save(pet);
    }
}