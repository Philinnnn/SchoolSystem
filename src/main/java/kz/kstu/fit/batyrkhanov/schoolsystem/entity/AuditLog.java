package kz.kstu.fit.batyrkhanov.schoolsystem.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime = LocalDateTime.now();

    @Column(length = 255)
    private String username; // может быть null, если действие до логина

    @Column(nullable = false, length = 64)
    private String action; // LOGIN, LOGOUT, PASSWORD_CHANGE, ROLE_CHANGE и т.д.

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String details;

    @Column(length = 64)
    private String ip;

    @Column(length = 128)
    private String sessionId;

    @Column(name = "log_uuid", length = 36, nullable = false, unique = true)
    private String logUuid = java.util.UUID.randomUUID().toString();

    public AuditLog() {}

    public AuditLog(String username, String action, String details, String ip, String sessionId) {
        this.username = username;
        this.action = action;
        this.details = details;
        this.ip = ip;
        this.sessionId = sessionId;
        this.eventTime = LocalDateTime.now();
        this.logUuid = java.util.UUID.randomUUID().toString();
    }

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getLogUuid() { return logUuid; }
    public void setLogUuid(String logUuid) { this.logUuid = logUuid; }
}
