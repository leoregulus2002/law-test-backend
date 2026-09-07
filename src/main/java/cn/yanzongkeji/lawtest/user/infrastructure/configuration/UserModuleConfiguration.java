package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class UserModuleConfiguration {
    private static final String PRODUCTION_WEB_ORIGIN = "https://fakao.yanzongkeji.cn";
    private static final String ANDROID_ORIGIN_PREFIX = "android:apk-key-hash:";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Profile("prod")
    InitializingBean productionAuthConfigurationValidator(AuthProperties properties) {
        return () -> {
            String secret = properties.jwt().secret();
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32
                    || secret.equals("dev-only-change-this-32-byte-secret"))
                throw new IllegalStateException("生产 JWT 密钥必须显式配置且至少为 32 个 UTF-8 字节");
            if (!"fakao.yanzongkeji.cn".equals(properties.webauthn().rpId()))
                throw new IllegalStateException("生产 WebAuthn RP ID 必须为 fakao.yanzongkeji.cn");
            boolean hasProductionWebOrigin = false;
            for (String origin : properties.webauthn().allowedOrigins()) {
                if (PRODUCTION_WEB_ORIGIN.equals(origin)) {
                    hasProductionWebOrigin = true;
                    continue;
                }
                if (origin.startsWith(ANDROID_ORIGIN_PREFIX)) {
                    validateAndroidOrigin(origin);
                    continue;
                }
                throw new IllegalStateException("生产 Web Origin 必须为 " + PRODUCTION_WEB_ORIGIN);
            }
            if (!hasProductionWebOrigin)
                throw new IllegalStateException("生产配置必须包含 Web Origin " + PRODUCTION_WEB_ORIGIN);
            boolean hasProductionFingerprint = properties.android().certificateSha256().stream()
                    .anyMatch(fingerprint -> fingerprint.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){31}")
                            && !isPlaceholderFingerprint(fingerprint));
            if (!hasProductionFingerprint)
                throw new IllegalStateException("生产配置至少需要一个非占位的冒号分隔 SHA-256 Android 证书指纹");
        };
    }

    private static boolean isPlaceholderFingerprint(String fingerprint) {
        String hex = fingerprint.replace(":", "").toLowerCase(Locale.ROOT);
        return hex.equals(hex.substring(0, 2).repeat(32));
    }

    private static void validateAndroidOrigin(String origin) {
        String encodedHash = origin.substring(ANDROID_ORIGIN_PREFIX.length());
        try {
            byte[] hash = Base64.getUrlDecoder().decode(encodedHash);
            if (hash.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(hash).equals(encodedHash))
                throw new IllegalStateException("生产 Android Origin 必须包含无填充 Base64URL 编码的 SHA-256 证书摘要");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("生产 Android Origin 必须包含无填充 Base64URL 编码的 SHA-256 证书摘要", exception);
        }
    }
}
