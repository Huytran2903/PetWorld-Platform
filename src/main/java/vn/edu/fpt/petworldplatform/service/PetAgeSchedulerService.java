package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.PetRepository;

import java.util.List;

@Service
public class PetAgeSchedulerService {

    @Autowired
    private PetRepository petRepository;


    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void autoUpdatePetAgeEveryMonth() {
        List<Pets> pets = petRepository.findAll();
        for (Pets pet : pets) {
            if (pet.getAgeMonths() != null) {
                pet.setAgeMonths(pet.getAgeMonths() + 1);
            }
        }
        petRepository.saveAll(pets);

        System.out.println("Đã tự động cộng thêm 1 tháng tuổi cho tất cả thú cưng!");
    }
}
