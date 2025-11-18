package kz.kstu.fit.batyrkhanov.schoolsystem.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

@Component
public class TelegramRuntimeConfig {
    private final String token;
    private final String username;
    private final long loginTokenTtlMinutes;

    public TelegramRuntimeConfig() {
        Dotenv dotenv = null;
        try { dotenv = Dotenv.configure().ignoreIfMissing().load(); } catch (Exception ignored) {}
        this.token = resolve(dotenv, "TELEGRAM_BOT_TOKEN", "");
        this.username = resolve(dotenv, "TELEGRAM_BOT_USERNAME", "twofactortgbot");
        String ttlStr = resolve(dotenv, "TELEGRAM_LOGIN_TOKEN_TTL_MINUTES", "5");
        long ttl = 5L;
        try { ttl = Long.parseLong(ttlStr); } catch (NumberFormatException ignored) {}
        this.loginTokenTtlMinutes = ttl;
    }

    private String resolve(Dotenv dotenv, String key, String def) {
        String v = System.getenv(key);
        if ((v == null || v.isBlank()) && dotenv != null) v = dotenv.get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public long getLoginTokenTtlMinutes() { return loginTokenTtlMinutes; }
    public boolean isEnabled() { return token != null && !token.isBlank(); }
}

