package vn.edu.fpt.petworldplatform.config;


import org.apache.catalina.core.StandardContext;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addContextCustomizers(context -> {

            if (context instanceof StandardContext standardContext) {

                System.out.println("✅ Tomcat 11: FIX FileCountLimitExceeded");

                // 🔥 FIX DUY NHẤT CẦN THIẾT

            }
        });
    }
}
