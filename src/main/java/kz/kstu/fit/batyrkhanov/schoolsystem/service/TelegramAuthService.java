package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import kz.kstu.fit.batyrkhanov.schoolsystem.config.TelegramRuntimeConfig;
import kz.kstu.fit.batyrkhanov.schoolsystem.entity.User;
import kz.kstu.fit.batyrkhanov.schoolsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

@Service
public class TelegramAuthService {

    private final UserRepository userRepository;
    private final TelegramRuntimeConfig runtimeConfig;
    private final Random random = new SecureRandom();
    private final AuditService auditService;
    private final SettingsService settingsService;

    public TelegramAuthService(UserRepository userRepository,
                               TelegramRuntimeConfig runtimeConfig,
                               AuditService auditService,
                               SettingsService settingsService) {
        this.userRepository = userRepository;
        this.runtimeConfig = runtimeConfig;
        this.auditService = auditService;
        this.settingsService = settingsService;
    }

    /**
     * Генерирует одноразовый 6-значный код входа и сохраняет его с TTL.
     */
    @Transactional
    public String generateLoginToken(User user, Long ttlMinutesOverride) {
        if (user == null) return null;
        String token = randomDigits(6);
        long ttl = ttlMinutesOverride != null && ttlMinutesOverride > 0 ? ttlMinutesOverride :
                settingsService.getLong("telegram_login_token_ttl_minutes").orElse(runtimeConfig.getLoginTokenTtlMinutes());
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusMinutes(ttl));
        userRepository.save(user);
        auditService.log("TG_LOGIN_TOKEN", user.getUsername(), "Generated Telegram login token TTL=" + ttl + "m");
        return token;
    }

    /**
     * Проверяет корректность кода входа (совпадение + не истёк).
     */
    public boolean validateLoginToken(User user, String token) {
        if (user == null || token == null) return false;
        if (user.getPasswordResetToken() == null) return false;
        if (!token.equals(user.getPasswordResetToken())) return false;
        if (user.getPasswordResetExpiry() == null || LocalDateTime.now().isAfter(user.getPasswordResetExpiry())) return false;
        return true;
    }

    /**
     * Одноразовое потребление токена (очистка после успешного входа).
     */
    @Transactional
    public void consumeLoginToken(User user) {
        if (user == null) return;
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
        auditService.log("TG_LOGIN_TOKEN_CONSUME", user.getUsername(), "Consumed Telegram login token");
    }

    /**
     * Генерация кода привязки Telegram (8 символов A-Z0-9).
     */
    @Transactional
    public String generateLinkCode(User user) {
        if (user == null) return null;
        String code = randomAlphaNum(8);
        user.setTelegramLinkCode(code);
        userRepository.save(user);
        auditService.log("TG_LINK_CODE", user.getUsername(), "Generated Telegram link code");
        return code;
    }

    /**
     * Возвращает существующий код или создаёт новый, если отсутствует.
     */
    @Transactional
    public String ensureLinkCode(User user) {
        if (user == null) return null;
        if (user.getTelegramLinkCode() == null || user.getTelegramLinkCode().length() < 4) {
            return generateLinkCode(user);
        }
        return user.getTelegramLinkCode();
    }

    /**
     * Привязываем Telegram chatId к пользователю.
     */
    @Transactional
    public boolean bindTelegram(User user, Long chatId) {
        if (user == null || chatId == null) return false;
        user.setTelegramId(chatId);
        user.setTelegramLinkCode(null); // одноразовый код больше не нужен
        userRepository.save(user);
        auditService.log("TG_LINK", user.getUsername(), "Bound Telegram chatId=" + chatId);
        return true;
    }

    /**
     * Отвязка Telegram.
     */
    @Transactional
    public boolean unbindTelegram(User user) {
        if (user == null) return false;
        user.setTelegramId(null);
        userRepository.save(user);
        auditService.log("TG_UNLINK", user.getUsername(), "Unbound Telegram");
        return true;
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }

    private String randomAlphaNum(int len) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }
}
