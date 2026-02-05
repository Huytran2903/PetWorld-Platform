package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.dto.PetCreateDTO;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.PetRepo;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PetService {

    @Autowired
    private PetRepo petRepo;

    @Autowired
    private CustomerRepo customerRepo;

    public List<Pets> getAllPets() {
        return petRepo.findAll();
    }

    public Pets getPetById(Long id) {
        return petRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng ID: " + id));
    }

    public void createPet(PetCreateDTO dto) {
        Pets pet = new Pets();

        pet.setName(dto.getName());
        pet.setPetType(dto.getSpecies());
        pet.setBreed(dto.getBreed());
        pet.setAgeMonths(dto.getAge());
        pet.setDescription(dto.getDescription());
        pet.setImageUrl(dto.getImageUrl());

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
}