package kz.kstu.fit.batyrkhanov.schoolsystem.telegram;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRequestStore {

    public enum Status { WAITING, APPROVED, DECLINED, EXPIRED }

    public static class LoginRequest {
        private final String id;
        private final Long userId;
        private final LocalDateTime created;
        private final LocalDateTime expiry;
        private volatile Status status;
        private volatile Integer telegramMessageId;
        private volatile Long chatId;

        public LoginRequest(Long userId, long ttlSeconds) {
            this.id = UUID.randomUUID().toString();
            this.userId = userId;
            this.created = LocalDateTime.now();
            this.expiry = this.created.plusSeconds(ttlSeconds);
            this.status = Status.WAITING;
        }
        public String getId() { return id; }
        public Long getUserId() { return userId; }
        public LocalDateTime getCreated() { return created; }
        public LocalDateTime getExpiry() { return expiry; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public Integer getTelegramMessageId() { return telegramMessageId; }
        public void setTelegramMessageId(Integer telegramMessageId) { this.telegramMessageId = telegramMessageId; }
        public Long getChatId() { return chatId; }
        public void setChatId(Long chatId) { this.chatId = chatId; }
    }

    private final Map<String, LoginRequest> store = new ConcurrentHashMap<>();

    public LoginRequest create(Long userId, long ttlSeconds) {
        LoginRequest lr = new LoginRequest(userId, ttlSeconds);
        store.put(lr.getId(), lr);
        return lr;
    }

    public LoginRequest get(String id) {
        LoginRequest lr = store.get(id);
        if (lr == null) return null;
        if (lr.getStatus() == Status.WAITING && LocalDateTime.now().isAfter(lr.getExpiry())) {
            lr.setStatus(Status.EXPIRED);
        }
        return lr;
    }

    public void approve(String id) {
        LoginRequest lr = get(id);
        if (lr != null && lr.getStatus() == Status.WAITING) lr.setStatus(Status.APPROVED);
    }

    public void decline(String id) {
        LoginRequest lr = get(id);
        if (lr != null && lr.getStatus() == Status.WAITING) lr.setStatus(Status.DECLINED);
    }

    public void remove(String id) { store.remove(id); }
}
