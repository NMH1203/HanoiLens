package vn.edu.usth.hanoilens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** The only Spring Boot entry point: one JVM, one artifact, one deployment. */
@SpringBootApplication
public class HanoiLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(HanoiLensApplication.class, args);
    }
}
