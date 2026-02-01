package vn.edu.fpt.petworldplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.VerificationToken;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.VerificationTokenRepo;

import java.time.LocalDateTime;

@Controller

public class VerificationController {
    @Autowired
    private VerificationTokenRepo tokenRepo;
    @Autowired
    private CustomerRepo customerRepo;

    @GetMapping("/verify")
    @Transactional
    public String verifyAccount(@RequestParam("token") String token) {
        VerificationToken verificationToken = tokenRepo.findByToken(token);

        //token k tồn tại
        if (verificationToken == null) {
            return "redirect:/login?verification_status=invalid";
        }


        //token hết hạn
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login?verification_status=expired";
        }

        Customer customer = verificationToken.getCustomer();

        customer.setIsActive(true);
        customerRepo.save(customer);

        tokenRepo.delete(verificationToken);

        return "redirect:/login?verification_status=success";
    }
}
