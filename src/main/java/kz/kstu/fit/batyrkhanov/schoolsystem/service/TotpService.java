package kz.kstu.fit.batyrkhanov.schoolsystem.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class TotpService {

    private static final Duration STEP = Duration.ofSeconds(30);

    public String generateSecret() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return new Base32().encodeToString(buffer).replace("=", "");
    }

    public String buildOtpAuthUrl(String issuer, String accountName, String secret) {
        String label = URLEncoder.encode(issuer + ":" + accountName, StandardCharsets.UTF_8);
        String iss = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30", label, secret, iss);
    }

    public boolean verifyCode(String secret, int code) {
        try {
            TimeBasedOneTimePasswordGenerator totp = new TimeBasedOneTimePasswordGenerator(STEP);
            byte[] keyBytes = new Base32().decode(secret);
            Key key = new SecretKeySpec(keyBytes, "HmacSHA1");

            Instant now = Instant.now();
            // window -1, 0, +1
            int current = totp.generateOneTimePassword(key, now);
            if (current == code) return true;

            int previous = totp.generateOneTimePassword(key, now.minus(STEP));
            if (previous == code) return true;

            int next = totp.generateOneTimePassword(key, now.plus(STEP));
            return next == code;
        } catch (Exception e) {
            return false;
        }
    }
}

