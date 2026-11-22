package vn.edu.usth.hanoilens.modules.destination.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tunables for POST /destinations/{id}/arrival-checks (application.yml: hanoilens.arrival.*). */
@ConfigurationProperties("hanoilens.arrival")
public record ArrivalProperties(int maxAccuracyMeters, int nearbyThresholdMeters, Duration maxClockSkew,
        Duration checkTtl) {
}
