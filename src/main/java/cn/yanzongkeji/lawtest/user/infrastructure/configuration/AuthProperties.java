package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "law-test.auth")
public record AuthProperties(Jwt jwt, WebAuthn webauthn, Android android) {

    public AuthProperties {
        Objects.requireNonNull(jwt, "jwt must not be null");
        Objects.requireNonNull(webauthn, "webauthn must not be null");
        Objects.requireNonNull(android, "android must not be null");
    }

    public record Jwt(
            String issuer,
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {

        public Jwt {
            requireNonBlank(issuer, "issuer");
            requireNonBlank(secret, "secret");
            Objects.requireNonNull(accessTokenTtl, "accessTokenTtl must not be null");
            Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl must not be null");
        }
    }

    public record WebAuthn(
            String rpId,
            String rpName,
            List<String> allowedOrigins,
            Duration ceremonyTtl) {

        public WebAuthn {
            requireNonBlank(rpId, "rpId");
            requireNonBlank(rpName, "rpName");
            allowedOrigins = requireNonBlankValues(allowedOrigins, "allowedOrigins");
            Objects.requireNonNull(ceremonyTtl, "ceremonyTtl must not be null");
        }
    }

    public record Android(String packageName, List<String> certificateSha256) {

        public Android {
            requireNonBlank(packageName, "packageName");
            certificateSha256 = requireNonBlankValues(certificateSha256, "certificateSha256");
        }
    }

    private static List<String> requireNonBlankValues(List<String> values, String name) {
        Objects.requireNonNull(values, name + " must not be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        values.forEach(value -> requireNonBlank(value, name + " entry"));
        return List.copyOf(values);
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
