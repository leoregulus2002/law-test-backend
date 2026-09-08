package cn.yanzongkeji.lawtest.user.infrastructure.redis;

import cn.yanzongkeji.lawtest.user.domain.port.AccessTokenDenylist;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisAccessTokenDenylist implements AccessTokenDenylist {
    private static final DefaultRedisScript<Long> REVOKE = RedisAuthSupport.script("access-revoke", Long.class);
    private final StringRedisTemplate redis;

    public RedisAccessTokenDenylist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void revoke(String token, Instant expiresAt) {
        Objects.requireNonNull(expiresAt, "Access Token 必须包含过期时间");
        RedisAuthSupport.required(() -> redis.execute(REVOKE, List.of(key(token)),
                Long.toString(expiresAt.toEpochMilli())));
    }

    @Override
    public boolean isRevoked(String token) {
        return RedisAuthSupport.required(() -> redis.hasKey(key(token)));
    }

    private static String key(String token) {
        return "auth:access:deny:" + RedisAuthSupport.digest(token);
    }
}
