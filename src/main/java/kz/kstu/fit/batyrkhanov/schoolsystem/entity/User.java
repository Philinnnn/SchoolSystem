package kz.kstu.fit.batyrkhanov.schoolsystem.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Column(name = "totp_secret")
    private String totpSecret;

    @Column(name = "totp_enabled")
    private Boolean totpEnabled = false;

    @Column(name = "telegram_id")
    private Long telegramId;

    @Column(name = "telegram_link_code")
    private String telegramLinkCode;

    @Column(name = "password_reset_token")
    private String passwordResetToken; // используем также как код входа через Telegram

    @Column(name = "password_reset_expiry")
    private LocalDateTime passwordResetExpiry;

    @Column(name = "archived")
    private Boolean archived = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public User() {}

    public User(String username, String password, String fullName) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
    }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public Boolean getTotpEnabled() { return totpEnabled != null ? totpEnabled : false; }
    public void setTotpEnabled(Boolean totpEnabled) { this.totpEnabled = totpEnabled; }
    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
    public String getTelegramLinkCode() { return telegramLinkCode; }
    public void setTelegramLinkCode(String telegramLinkCode) { this.telegramLinkCode = telegramLinkCode; }
    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }
    public LocalDateTime getPasswordResetExpiry() { return passwordResetExpiry; }
    public void setPasswordResetExpiry(LocalDateTime passwordResetExpiry) { this.passwordResetExpiry = passwordResetExpiry; }
    public Boolean getArchived() { return archived != null ? archived : false; }
    public void setArchived(Boolean archived) { this.archived = archived; }
}
