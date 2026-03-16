package vn.edu.fpt.petworldplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.petworldplatform.entity.AuthProvider;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.entity.VerificationToken;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;
import vn.edu.fpt.petworldplatform.repository.VerificationTokenRepo;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
                .orElseThrow(() -> new Exception("Email has not exist!"));

        if (customer.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new Exception("Email này được liên kết với Google. Vui lòng đăng nhập bằng nút Google!");
        }

        VerificationToken oldToken = verificationTokenRepo.findByCustomer(customer);
        if (oldToken != null) {
            verificationTokenRepo.delete(oldToken);
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));

        VerificationToken newToken = new VerificationToken(otp, customer);
        verificationTokenRepo.save(newToken);

        String subject = "Mã xác thực (OTP) đặt lại mật khẩu - Pet World";
        String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #ddd; max-width: 600px; margin: 0 auto; border-radius: 8px;'>"
                + "<div style='text-align: center; margin-bottom: 20px;'>"
                + "  <h2 style='color: #dc3545; margin: 0;'>Đặt Lại Mật Khẩu</h2>"
                + "</div>"
                + "<p>Xin chào <strong>" + customer.getFullName() + "</strong>,</p>"
                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản tại <strong>Pet World</strong>. Đây là mã xác thực (OTP) của bạn:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "  <span style='font-size: 28px; font-weight: bold; background-color: #f8f9fa; padding: 12px 24px; border-radius: 6px; color: #dc3545; letter-spacing: 5px; border: 1px dashed #dc3545;'>" + otp + "</span>"
                + "</div>"
                + "<p style='color: #555;'>Mã OTP này có hiệu lực trong vòng <strong>"
                + newToken.getExpiryDate()
                + " phút</strong>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                + "</div>";

        emailService.sendEmail(customer.getEmail(), subject, htmlContent);
    }

    public Customer getByResetPasswordToken(String token) {
        System.out.println("DEBUG: Đang tìm token: " + token);

        Optional<VerificationToken> tokenOpt = verificationTokenRepo.findByToken(token);

        if (tokenOpt.isEmpty()) {
            System.out.println("DEBUG: -> Không tìm thấy Token trong bảng verification_tokens!");
            return null;
        }

        VerificationToken verificationToken = tokenOpt.get();
        System.out.println("DEBUG: -> Tìm thấy Token. Hết hạn lúc: " + verificationToken.getExpiryDate());
        System.out.println("DEBUG: -> Thời gian hiện tại: " + LocalDateTime.now());

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            System.out.println("DEBUG: -> Token ĐÃ HẾT HẠN!");
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

    //OanhTP - findIdByUsername
    public Integer findIdByUsername(String username) {
        // Tìm khách hàng theo username
        return customerRepo.findByUsername(username)
                .map(customer -> customer.getCustomerId()) // Lấy ID từ đối tượng Customer
                .orElseThrow(() -> new RuntimeException("Customer not found with username: " + username));
    }

    public Integer findIdByEmail(String email) {
        return customerRepo.findByEmail(email)
                .map(Customer::getCustomerId)
                .orElse(null);
    }

    public Page<Customer> getCustomersWithPaginationAndSearch(String keyword, int page, int size) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("customerId").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            return customerRepo.searchCustomers(keyword.trim(), pageable);
        }
        return customerRepo.findAll(pageable);
    }
}