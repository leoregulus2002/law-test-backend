package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "law-test.auth.login-protection")
public record LoginProtectionProperties(
        @DefaultValue("5") int passwordFailureLimit,
        @DefaultValue("PT15M") Duration passwordFailureWindow,
        @DefaultValue("PT15M") Duration passwordLockDuration,
        @DefaultValue("20") int accountLimit,
        @DefaultValue("100") int ipLimit,
        @DefaultValue("PT1M") Duration rateWindow) {
    public LoginProtectionProperties {
        if (passwordFailureLimit < 1 || accountLimit < 1 || ipLimit < 1)
            throw new IllegalArgumentException("登录防护阈值必须为正数");
        requirePositive(passwordFailureWindow);
        requirePositive(passwordLockDuration);
        requirePositive(rateWindow);
    }

    private static void requirePositive(Duration duration) {
        if (duration == null || duration.toMillis() < 1)
            throw new IllegalArgumentException("登录防护有效期至少为 1 毫秒");
    }
}
