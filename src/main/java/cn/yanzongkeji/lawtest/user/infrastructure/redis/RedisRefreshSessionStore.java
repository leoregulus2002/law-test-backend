package cn.yanzongkeji.lawtest.user.infrastructure.redis;

import cn.yanzongkeji.lawtest.user.domain.model.RefreshSession;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.RefreshSessionStore;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

/** 多键脚本要求单实例或 Sentinel Redis，不能直接用于 Redis Cluster。 */
@Repository
public class RedisRefreshSessionStore implements RefreshSessionStore {
    private static final DefaultRedisScript<Long> CREATE = RedisAuthSupport.script("refresh-create", Long.class);
    private static final DefaultRedisScript<Long> ROTATE = RedisAuthSupport.script("refresh-rotate", Long.class);
    private static final DefaultRedisScript<Long> REVOKE = RedisAuthSupport.script("refresh-revoke", Long.class);
    private final StringRedisTemplate redis;

    public RedisRefreshSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void create(RefreshSession session) {
        long result = RedisAuthSupport.required(() -> redis.execute(CREATE,
                List.of(key(session.tokenHash()), familyKey(session.tokenFamilyId())), arguments(session)));
        if (result != 1)
            throw new IllegalStateException("无法创建刷新会话");
    }

    @Override
    public Optional<RefreshSession> find(byte[] tokenHash) {
        Map<Object, Object> values = RedisAuthSupport.required(() -> redis.opsForHash().entries(key(tokenHash)));
        if (values.isEmpty())
            return Optional.empty();
        String previous = (String) values.get("previous");
        String used = (String) values.get("used");
        return Optional.of(new RefreshSession(UUID.fromString((String) values.get("id")),
                new UserId(Long.parseLong((String) values.get("user"))), tokenHash,
                UUID.fromString((String) values.get("family")),
                previous.isEmpty() ? null : UUID.fromString(previous),
                Instant.ofEpochMilli(Long.parseLong((String) values.get("created"))),
                Instant.ofEpochMilli(Long.parseLong((String) values.get("expires"))),
                used.isEmpty() ? null : Instant.ofEpochMilli(Long.parseLong(used)), null));
    }

    @Override
    public boolean rotate(RefreshSession current, RefreshSession replacement) {
        if (!current.tokenFamilyId().equals(replacement.tokenFamilyId())
                || !current.userId().equals(replacement.userId())
                || !current.id().equals(replacement.previousSessionId()))
            throw new IllegalArgumentException("刷新会话必须属于同一用户及家族");
        return RedisAuthSupport.required(() -> redis.execute(ROTATE,
                List.of(key(current.tokenHash()), key(replacement.tokenHash()),
                        familyKey(current.tokenFamilyId())), arguments(replacement))) == 1;
    }

    @Override
    public void revokeFamily(byte[] tokenHash) {
        RedisAuthSupport.required(() -> redis.execute(REVOKE, List.of(key(tokenHash))));
    }

    private static String key(byte[] tokenHash) {
        return "auth:refresh:" + HexFormat.of().formatHex(tokenHash);
    }

    private static String familyKey(UUID familyId) {
        return "auth:refresh-family:" + familyId;
    }

    private static Object[] arguments(RefreshSession session) {
        return new Object[] { session.id().toString(), Long.toString(session.userId().value()),
                session.tokenFamilyId().toString(),
                session.previousSessionId() == null ? "" : session.previousSessionId().toString(),
                Long.toString(session.createdAt().toEpochMilli()), Long.toString(session.expiresAt().toEpochMilli()) };
    }
}
