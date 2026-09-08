package cn.yanzongkeji.lawtest.user.infrastructure.redis;

import cn.yanzongkeji.lawtest.user.application.exception.LoginRateLimitedException;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.LoginProtection;
import cn.yanzongkeji.lawtest.user.infrastructure.configuration.LoginProtectionProperties;
import java.util.List;
import java.util.Locale;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisLoginProtection implements LoginProtection {
    private static final DefaultRedisScript<Long> RATE = RedisAuthSupport.script("login-rate", Long.class);
    private static final DefaultRedisScript<Long> FAILURE = RedisAuthSupport.script("password-failure", Long.class);
    private static final DefaultRedisScript<Long> CLEAR = RedisAuthSupport.script("password-clear", Long.class);
    private final StringRedisTemplate redis;
    private final LoginProtectionProperties properties;

    public RedisLoginProtection(StringRedisTemplate redis, LoginProtectionProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public void checkAccountRateLimit(String username) {
        String normalized = username == null ? "" : username.strip().toLowerCase(Locale.ROOT);
        checkRate("auth:login:account:" + RedisAuthSupport.digest(normalized), properties.accountLimit());
    }

    @Override
    public void checkIpRateLimit(String ip) {
        checkRate("auth:login:ip:" + RedisAuthSupport.digest(ip == null ? "unknown" : ip), properties.ipLimit());
    }

    private void checkRate(String key, int limit) {
        long retryMillis = RedisAuthSupport.required(() -> redis.execute(RATE, List.of(key),
                Integer.toString(limit), Long.toString(properties.rateWindow().toMillis())));
        if (retryMillis > 0)
            throw new LoginRateLimitedException((retryMillis + 999) / 1000);
    }

    @Override
    public boolean isLocked(UserId userId) {
        return RedisAuthSupport.required(() -> redis.hasKey(lockKey(userId)));
    }

    @Override
    public void recordPasswordFailure(UserId userId) {
        RedisAuthSupport.required(() -> redis.execute(FAILURE, List.of(failureKey(userId), lockKey(userId)),
                Integer.toString(properties.passwordFailureLimit()),
                Long.toString(properties.passwordFailureWindow().toMillis()),
                Long.toString(properties.passwordLockDuration().toMillis())));
    }

    @Override
    public boolean clearPasswordFailures(UserId userId) {
        return RedisAuthSupport.required(() -> redis.execute(CLEAR,
                List.of(failureKey(userId), lockKey(userId)))) == 1;
    }

    private static String failureKey(UserId id) {
        return "auth:password:failures:" + id.value();
    }

    private static String lockKey(UserId id) {
        return "auth:password:lock:" + id.value();
    }
}
