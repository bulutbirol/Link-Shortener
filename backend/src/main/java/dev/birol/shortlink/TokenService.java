package dev.birol.shortlink;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class TokenService {
    private final byte[] secret;
    TokenService(@Value("${app.token-secret}") String secret) {
        if (secret.length() < 32) throw new IllegalArgumentException("app.token-secret must be at least 32 characters");
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    String issue(Long userId) {
        String body = userId + "." + Instant.now().plusSeconds(7 * 86400).getEpochSecond();
        return body + "." + sign(body);
    }

    Long read(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new UnauthorizedException();
        String token = header.substring(7);
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new UnauthorizedException();
        String body = parts[0] + "." + parts[1];
        byte[] given;
        try {
            given = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(given, Base64.getUrlDecoder().decode(sign(body)))) throw new UnauthorizedException();
            if (Instant.now().getEpochSecond() >= Long.parseLong(parts[1])) throw new UnauthorizedException();
            return Long.parseLong(parts[0]);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException();
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign token", ex);
        }
    }
}
