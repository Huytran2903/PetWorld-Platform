package vn.edu.fpt.petworldplatform.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

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

            helper.setFrom("huytpn.ce190719@gmail.com", "Pet World Support");
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
    public void sendVerificationEmail(String to, String token) {
        String subject = "Xác thực tài khoản Pet World";
        String verifyUrl = baseUrl + "/verify?token=" + token;

        String content = "<div style='font-family: Arial; padding: 20px; border: 1px solid #ddd;'>"
                + "<h2 style='color: #ff9900;'>Chào mừng đến với Pet World!</h2>"
                + "<p>Cảm ơn bạn đã đăng ký. Vui lòng nhấn vào nút bên dưới để kích hoạt tài khoản:</p>"
                + "<a href='" + verifyUrl + "' style='background-color: #ff9900; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Xác Thực Ngay</a>"
                + "<p>Hoặc copy link này: " + verifyUrl + "</p>"
                + "</div>";

        sendEmail(to, subject, content);
    }

    /**
     * Gửi mail quên mật khẩu (Forgot Password)
     * Hàm này tương thích với cách gọi ở CustomerService bước trước
     */
    public void sendResetPasswordEmail(String to, String token) {
        String subject = "Yêu cầu đặt lại mật khẩu";
        String resetUrl = baseUrl + "/reset-password?token=" + token;

        String content = "<div style='font-family: Arial; padding: 20px; border: 1px solid #ddd;'>"
                + "<h2 style='color: #dc3545;'>Đặt Lại Mật Khẩu</h2>"
                + "<p>Bạn vừa yêu cầu đổi mật khẩu. Nhấn vào nút dưới đây để tạo mật khẩu mới:</p>"
                + "<a href='" + resetUrl + "' style='background-color: #dc3545; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Đổi Mật Khẩu</a>"
                + "<p>Link này sẽ hết hạn sau 24 giờ.</p>"
                + "</div>";

        sendEmail(to, subject, content);
    }
}