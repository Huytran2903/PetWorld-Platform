package vn.edu.fpt.petworldplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

    // 1. BEAN MỚI (BẮT BUỘC): Để AuthController có thể gọi authenticationManager.authenticate()
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Tắt CSRF để dễ test (Production nên bật lại)

                // --- PHÂN QUYỀN ---
                .authorizeHttpRequests(auth -> auth
                        // A. Link Tĩnh
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()

                        // B. Link Public
                        .requestMatchers("/", "/home", "/index").permitAll()
                        .requestMatchers("/login", "/register", "/do-register", "/verify").permitAll()

                        .requestMatchers("/do-login").permitAll()

                        // C. Phân quyền
//                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF") // Chỉ Staff/Admin mới vào được Admin
//                        .requestMatchers("/profile/**").authenticated() // Đăng nhập là vào được

                        // D. Còn lại khóa hết
                        .anyRequest().authenticated()
                )

                // --- CẤU HÌNH FORM LOGIN ---

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login-security-check")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                // --- GOOGLE LOGIN ---
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(googleLoginSuccessHandler)
                )

                // --- LOGOUT ---
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}