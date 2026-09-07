package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class UserModuleConfiguration {
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
            boolean hasWebOrigin = false;
            for (String origin : properties.webauthn().allowedOrigins()) {
                if (origin.startsWith("android:apk-key-hash:"))
                    continue;
                URI uri = URI.create(origin);
                if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                        || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                        || (uri.getRawPath() != null && !uri.getRawPath().isEmpty()))
                    throw new IllegalStateException("生产 Web Origin 必须为不含路径的 HTTPS Origin");
                hasWebOrigin = true;
            }
            if (!hasWebOrigin)
                throw new IllegalStateException("生产配置至少需要一个 HTTPS Web Origin");
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
}
