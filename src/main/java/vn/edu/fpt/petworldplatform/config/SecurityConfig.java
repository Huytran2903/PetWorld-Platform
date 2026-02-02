package vn.edu.fpt.petworldplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                // --- 1. PHÂN QUYỀN TRUY CẬP ---
                .authorizeHttpRequests(auth -> auth
                        // A. Các link Tĩnh (CSS, JS, Ảnh)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()

                        // B. Các trang PUBLIC (Không cần đăng nhập)
                        .requestMatchers("/", "/home", "/index").permitAll()
                        .requestMatchers("/login", "/register", "/do-register", "/do-login").permitAll()

                        // QUAN TRỌNG: Mở khóa link verify email
                        .requestMatchers("/verify").permitAll()

                        // C. Các trang còn lại -> BẮT BUỘC ĐĂNG NHẬP
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login-security-check")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(googleLoginSuccessHandler) // Xử lý logic lưu user Google
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}