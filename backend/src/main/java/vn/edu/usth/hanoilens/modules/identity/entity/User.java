package vn.edu.usth.hanoilens.modules.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    public static final String ROLE_USER = "USER";
    public static final String STATUS_ACTIVE = "ACTIVE";

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "default_location_display", nullable = false, length = 16)
    private String defaultLocationDisplay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(UUID id, String email, String passwordHash, String displayName, String role, String status,
            String locale, String defaultLocationDisplay, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
        this.locale = locale;
        this.defaultLocationDisplay = defaultLocationDisplay;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getLocale() { return locale; }
    public String getDefaultLocationDisplay() { return defaultLocationDisplay; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isActive() { return STATUS_ACTIVE.equals(status); }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setDefaultLocationDisplay(String value) { this.defaultLocationDisplay = value; }
}
