package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.VerificationToken;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.VerificationTokenRepo;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepo;
    private final VerificationTokenRepo verificationTokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otpValue = 100000 + random.nextInt(900000);
        return String.valueOf(otpValue);
    }

    @Transactional
    public void registerNewCustomer(Customer customer) {
        customer.setPasswordHash(passwordEncoder.encode(customer.getPasswordHash()));
        customer.setIsActive(false);

        Customer savedCustomer = customerRepo.save(customer);

        String otp = generateOtp();

        VerificationToken tokenEntity = new VerificationToken();
        tokenEntity.setToken(otp);
        tokenEntity.setCustomer(savedCustomer);
        tokenEntity.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        verificationTokenRepo.save(tokenEntity);

        emailService.sendVerificationEmail(savedCustomer, otp);
    }

    public boolean verifyOtp(String email, String otp) {
        VerificationToken tokenEntity = verificationTokenRepo.findByToken(otp).orElse(null);
        if (tokenEntity != null
                && tokenEntity.getCustomer().getEmail().equals(email)
                && tokenEntity.getExpiryDate().isAfter(LocalDateTime.now())) {

            // Kích hoạt tài khoản
            Customer customer = tokenEntity.getCustomer();
            customer.setIsActive(true);
            customerRepo.save(customer);

            // Xóa token sau khi dùng xong (tuỳ chọn)
            verificationTokenRepo.delete(tokenEntity);
            return true;
        }
        return false;
    }

    public Optional<Customer> login(String usernameOrEmail, String rawPassword) {
        Optional<Customer> customerOpt = customerRepo.findByEmail(usernameOrEmail);

        if (customerOpt.isEmpty()) {
            customerOpt = customerRepo.findByUsername(usernameOrEmail);
        }

        if (customerOpt.isPresent()) {
            if (passwordEncoder.matches(rawPassword, customerOpt.get().getPasswordHash())) {
                return customerOpt;
            }
        }
        return Optional.empty();
    }

    @Transactional
    public String verifyEmailToken(String token) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepo.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return "invalid";
        }

        VerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "expired";
        }

        Customer customer = verificationToken.getCustomer();
        if (customer != null) {
            customer.setIsActive(true);
            customerRepo.save(customer);
        }

        verificationTokenRepo.delete(verificationToken);

        return "success";
    }

    public void sendResetPasswordEmail(String email) throws Exception {
        Customer customer = customerRepo.findByEmail(email)
                .orElseThrow(() -> new Exception("Email has not exist"));

        VerificationToken oldToken = verificationTokenRepo.findByCustomer(customer);
        if (oldToken != null) {
            verificationTokenRepo.delete(oldToken);
        }

        String tokenString = UUID.randomUUID().toString();
        VerificationToken newToken = new VerificationToken(tokenString, customer);
        verificationTokenRepo.save(newToken);

        String resetLink = "http://localhost:8080/reset-password?token=" + tokenString;
        emailService.sendEmail(customer.getEmail(), "Đặt lại mật khẩu",
                "Click vào link sau để đặt lại mật khẩu: " + resetLink);
    }

    public Customer getByResetPasswordToken(String token) {

        Optional<VerificationToken> tokenOpt = verificationTokenRepo.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return null;
        }

        VerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return null;
        }

        return verificationToken.getCustomer();
    }

    @Transactional
    public void updatePassword(Customer customer, String newRawPassword) {
        String encodedPass = passwordEncoder.encode(newRawPassword);
        customer.setPasswordHash(encodedPass);
        customerRepo.save(customer);

        VerificationToken token = verificationTokenRepo.findByCustomer(customer);
        if (token != null) {
            verificationTokenRepo.delete(token);
        }
    }

    public List<Customer> getAllCustomer() {
        return customerRepo.findAll();
    }

    public void deleteCustomer(int id) {
        if (!customerRepo.existsById(id)) {
            throw new RuntimeException("Customer not found with id: " + id);
        }
        customerRepo.deleteById(id);
    }

    public void updateCustomerStatus(int id, boolean newStatus) {
        Customer customer = customerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        customer.setIsActive(newStatus);

        customerRepo.save(customer);
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepo.findByEmail(email);
    }

    public void updateCustomer(Customer customer) {
        customerRepo.save(customer);
    }

    public boolean checkEmailExists(String email) {
        return customerRepo.existsByEmail(email);
    }

    public Optional<Customer> findById(int id) {
        return customerRepo.findById(id);
    }

    public boolean verifyOldPassword(Customer customer, String oldRawPassword) {
        return passwordEncoder.matches(oldRawPassword, customer.getPasswordHash());
    }
}