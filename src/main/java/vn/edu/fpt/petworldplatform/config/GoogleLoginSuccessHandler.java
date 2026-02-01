package vn.edu.fpt.petworldplatform.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.edu.fpt.petworldplatform.entity.AuthProvider;
import vn.edu.fpt.petworldplatform.entity.Customer;
import vn.edu.fpt.petworldplatform.repository.CustomerRepo;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CustomerRepo customerRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        Customer customer = customerRepository.findByEmail(email).orElse(null);

        if (customer == null) {
            customer = new Customer();
            customer.setEmail(email);
            customer.setFullName(name);
            customer.setAuthProvider(AuthProvider.GOOGLE);

            String dummyPassword = "GoogleAuth" + UUID.randomUUID().toString();

            customer.setPasswordHash(passwordEncoder.encode(dummyPassword));

            String baseUsername = email.split("@")[0];
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 5);
            String generatedUsername = baseUsername + "_" + uniqueSuffix;

            if (generatedUsername.length() > 50) {
                generatedUsername = generatedUsername.substring(0, 50);
            }

            customer.setUsername(generatedUsername);

            customerRepository.save(customer);
        } else {

            customer.setAuthProvider(AuthProvider.GOOGLE);
            customer.setFullName(name);
            customerRepository.save(customer);
        }

        request.getSession().setAttribute("loggedInAccount", customer);

        setDefaultTargetUrl("/");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}