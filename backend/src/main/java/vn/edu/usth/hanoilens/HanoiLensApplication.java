package vn.edu.usth.hanoilens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** The only Spring Boot entry point. Business modules live in {@code vn.edu.usth.hanoilens.modules.*}. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class HanoiLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(HanoiLensApplication.class, args);
    }
}
