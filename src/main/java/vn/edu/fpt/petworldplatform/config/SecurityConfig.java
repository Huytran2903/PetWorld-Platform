package vn.edu.fpt.petworldplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@Configuration
@EnableWebSecurity
@EnableAsync
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;

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
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                )
                // --- PHÂN QUYỀN ---
                .authorizeHttpRequests(auth -> auth
                        // A. Link Tĩnh
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()

                        // B. Link Public
                        .requestMatchers("/", "/home", "/index").permitAll()
                        .requestMatchers("/login", "/register", "/do-register", "/verify").permitAll()
                        .requestMatchers("/do-login").permitAll()

                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/reset-password/**", "/forgot-password", "/verify-forgot-password-otp").permitAll()
                        .requestMatchers("/staff/**").permitAll()
                        // --------------------

                        .requestMatchers("/profile/**").authenticated()

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