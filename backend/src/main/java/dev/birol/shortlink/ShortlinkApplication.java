package dev.birol.shortlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ShortlinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShortlinkApplication.class, args);
    }
}
