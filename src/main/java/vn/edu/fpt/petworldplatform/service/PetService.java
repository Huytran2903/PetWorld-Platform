package vn.edu.fpt.petworldplatform.service;

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
    @Autowired private CustomerRepo customerRepo; // Dùng repo bạn đã có

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

        if ("shop".equals(dto.getOwnerType())) {
            // Pet của Shop -> Có giá bán, Không có chủ
            pet.setPrice(dto.getPrice());
            pet.setOwner(null);
            pet.setAvailable(true);
        } else {
            // Pet của Khách -> Không giá bán, Phải có chủ
            Customer owner = customerRepo.findById(dto.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found ID: " + dto.getOwnerId()));
            pet.setOwner(owner);
            pet.setPrice(null);
            pet.setAvailable(false); // Đã có chủ thì không bán
        }
        petRepo.save(pet);
    }
}