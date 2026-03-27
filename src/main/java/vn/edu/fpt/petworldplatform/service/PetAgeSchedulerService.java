package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.repository.PetRepo;

import java.util.List;

@Service
public class PetAgeSchedulerService {

    @Autowired
    private PetRepo petRepository;

    // Chạy vào lúc 00:00:00 ngày mùng 1 mỗi tháng
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void autoUpdatePetAgeEveryMonth() {
        // Có 2 cách update:
        // Cách A: Cập nhật trực tiếp bằng câu lệnh SQL (nhanh hơn)
        // petRepository.incrementAgeMonthsForAllPets();

        // Cách B: Dùng JPA (dễ hiểu hơn nếu data không quá lớn)
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
