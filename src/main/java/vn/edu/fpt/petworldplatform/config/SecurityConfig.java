package vn.edu.fpt.petworldplatform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import vn.edu.fpt.petworldplatform.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableAsync
@RequiredArgsConstructor
//@EnableMethodSecurity
public class SecurityConfig {

    private final GoogleLoginSuccessHandler googleLoginSuccessHandler;
    private final CustomUserDetailsService customUserDetailsService;

    private final CustomLoginSuccessHandler customLoginSuccessHandler;

    private final CustomLoginFailureHandler customLoginFailureHandler;

    @Value("${petworld.security.remember-me.key}")
    private String rememberMeKey;

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
        http.csrf(csrf -> csrf.disable()).securityContext(context -> context.securityContextRepository(new HttpSessionSecurityContextRepository())).httpBasic(Customizer.withDefaults())
                // --- PHÂN QUYỀN ---
                .authorizeHttpRequests(auth -> auth
                        // A. Link Tĩnh
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/webjars/**").permitAll()

                        // B. Link Public
                        .requestMatchers("/", "/home", "/index").permitAll().requestMatchers("/login", "/register", "/do-register", "/verify").permitAll().requestMatchers("/do-login").permitAll()
                        .requestMatchers("/cart/momo-notify").permitAll()

                        .requestMatchers("/uploads/**").permitAll()

                        .requestMatchers("/reset-password/**", "/forgot-password", "/verify-forgot-password-otp").permitAll()
                        // --------------------

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/staff/**").hasRole("STAFF")
                        // C. Chặn staff truy cập trang customer
                        // Cho phép CUSTOMER (ROLE_CUSTOMER) hoặc OIDC_USER
                        .requestMatchers("/customer/**", "/cart/**", "/appointment/**")
                        .hasAnyAuthority("ROLE_CUSTOMER", "OIDC_USER")

                        .requestMatchers("/profile/**").authenticated()

                        // D. Còn lại khóa hết
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        //Security gọi DaoAuthenticationProvider tự goji CustomUserDetailService xử lí chia luồng staff, customer
                        //với set ROLE-....
                        //DaoAuthenticationProvider sẽ dựa vào passwordEncoder coi hash mk theo phương pháp nào rồi quét
                        //check mk có đúng hay không
                        .successHandler(customLoginSuccessHandler)
                        .failureHandler(customLoginFailureHandler)
                        .permitAll()
                )

                // --- REMEMBER ME ---
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .userDetailsService(customUserDetailsService)
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )

                // --- GOOGLE LOGIN ---
                .oauth2Login(oauth2 -> oauth2.loginPage("/login").successHandler(googleLoginSuccessHandler))

                // --- LOGOUT ---
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout").deleteCookies("JSESSIONID").permitAll());

        return http.build();
    }
}