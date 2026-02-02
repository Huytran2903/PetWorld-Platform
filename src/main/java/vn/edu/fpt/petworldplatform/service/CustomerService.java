package vn.edu.fpt.petworldplatform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.VerificationToken;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.VerificationTokenRepo;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepo customerRepo;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private VerificationTokenRepo verificationTokenRepo;

    @Autowired
    private EmailService emailService;

    //Register - HuyTPN
    @Transactional
    public void registerNewCustomer(Customer customer) {
        customer.setPasswordHash(passwordEncoder.encode(customer.getPasswordHash()));
        customer.setIsActive(false);
        customerRepo.save(customer);

        VerificationToken token = new VerificationToken(customer);
        verificationTokenRepo.save(token);

        emailService.sendVerificationEmail(customer.getEmail(), token.getToken());
    }

    //Login - HuyTPN
    public Optional<Customer> login(String email, String rawPassword) {
        Optional<Customer> customerOpt = customerRepo.findByEmail(email);

        if (customerOpt.isPresent()) {
            if (passwordEncoder.matches(rawPassword, customerOpt.get().getPasswordHash())) {
                return customerOpt;
            }
        }
        return Optional.empty();
    }

    public void updateCustomer(Customer customer) {
        customerRepo.save(customer);
    }

    public boolean checkEmailExists(String email) {
        return customerRepo.existsByEmail(email);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepo.findById(id);
    }

}