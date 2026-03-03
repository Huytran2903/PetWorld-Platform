package vn.edu.fpt.petworldplatform.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import vn.edu.fpt.petworldplatform.entity.Customer;

import java.io.UnsupportedEncodingException;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    private String baseUrl = "http://localhost:8080";

    /**
     * Phương thức gửi mail chung (Hỗ trợ HTML)
     *
     * @Async: Giúp chạy ngầm, không làm người dùng phải chờ mail gửi xong mới load trang web.
     */
    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("petworldfpt@gmail.com", "Pet World Support");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = cho phép HTML

            mailSender.send(message);
            System.out.println("Mail sent successfully to " + to);

        } catch (MessagingException | UnsupportedEncodingException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            // Có thể log lỗi vào database nếu cần
        }
    }

    /**
     * Gửi mail xác thực tài khoản (Register)
     */
    public void sendVerificationEmail(Customer customer, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom("your-shop@example.com");
        message.setTo(customer.getEmail());
        message.setSubject("Xác thực tài khoản - Mã OTP của bạn");

        String content = "Xin chào " + customer.getFullName() + ",\n\n"
                + "Cảm ơn bạn đã đăng ký. Mã xác thực (OTP) của bạn là: " + otp + "\n"
                + "Mã này sẽ hết hạn sau 5 phút.\n\n"
                + "Trân trọng,\nPetWorld Team.";

        message.setText(content);

        mailSender.send(message);
    }




}