package cn.yanzongkeji.lawtest.user.infrastructure.redis;

import cn.yanzongkeji.lawtest.user.application.exception.CeremonyUnavailableException;
import cn.yanzongkeji.lawtest.user.domain.model.AuthCeremony;
import cn.yanzongkeji.lawtest.user.domain.port.AuthCeremonyRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** Redis-backed, TTL-enforced storage for one-time WebAuthn ceremonies. */
@Repository
@RequiredArgsConstructor
public class RedisAuthCeremonyRepository implements AuthCeremonyRepository {
    private static final String KEY_PREFIX = "webauthn:ceremony:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void create(AuthCeremony ceremony) {
        Objects.requireNonNull(ceremony, "ceremony must not be null");
        Duration ttl = Duration.between(Instant.now(), ceremony.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            throw new CeremonyUnavailableException();
        }
        redis.opsForValue().set(key(ceremony.id()), serialize(ceremony), ttl);
    }

    @Override
    public AuthCeremony consume(UUID id, AuthCeremony.CeremonyType type, Instant now) {
        Objects.requireNonNull(id, "ceremony ID must not be null");
        Objects.requireNonNull(type, "ceremony type must not be null");
        Objects.requireNonNull(now, "current time must not be null");
        String serialized = redis.opsForValue().getAndDelete(key(id));
        if (serialized == null) {
            throw new CeremonyUnavailableException();
        }
        AuthCeremony ceremony = deserialize(serialized);
        if (ceremony.ceremonyType() != type || !ceremony.expiresAt().isAfter(now)) {
            throw new CeremonyUnavailableException();
        }
        return ceremony;
    }

    private static String key(UUID id) {
        return KEY_PREFIX + id;
    }

    private String serialize(AuthCeremony ceremony) {
        try {
            return objectMapper.writeValueAsString(ceremony);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化 WebAuthn ceremony", exception);
        }
    }

    private AuthCeremony deserialize(String serialized) {
        try {
            return objectMapper.readValue(serialized, AuthCeremony.class);
        } catch (JsonProcessingException exception) {
            throw new CeremonyUnavailableException();
        }
    }
}
