package cn.yanzongkeji.lawtest.user.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.user.application.exception.CeremonyUnavailableException;
import cn.yanzongkeji.lawtest.user.domain.model.AuthCeremony;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.AuthCeremonyRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AuthCeremonyDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.AuthCeremonyMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresAuthCeremonyRepository implements AuthCeremonyRepository {
    private final AuthCeremonyMapper mapper;

    @Override
    public void create(AuthCeremony ceremony) {
        Objects.requireNonNull(ceremony, "ceremony must not be null");
        AuthCeremonyDO data = new AuthCeremonyDO();
        data.setId(ceremony.id());
        data.setChallenge(ceremony.challenge());
        data.setCeremonyType(ceremony.ceremonyType().name());
        data.setUserId(ceremony.userId() == null ? null : ceremony.userId().value());
        data.setOptions(ceremony.optionsJson());
        data.setDummy(ceremony.dummy());
        data.setCreatedAt(ceremony.createdAt());
        data.setExpiresAt(ceremony.expiresAt());
        data.setConsumedAt(ceremony.consumedAt());
        if (mapper.insert(data) != 1) {
            throw new IllegalStateException("WebAuthn ceremony creation failed");
        }
    }

    @Override
    public AuthCeremony consume(UUID id, AuthCeremony.CeremonyType type, Instant now) {
        Objects.requireNonNull(id, "ceremony ID must not be null");
        Objects.requireNonNull(type, "ceremony type must not be null");
        Objects.requireNonNull(now, "current time must not be null");
        AuthCeremonyDO data = mapper.consume(id, type.name(), now);
        if (data == null) {
            throw new CeremonyUnavailableException();
        }
        // consumed_at is intentionally not returned as usable state: this is the pre-consumption payload.
        return new AuthCeremony(data.getId(), data.getChallenge(), AuthCeremony.CeremonyType.valueOf(data.getCeremonyType()),
                data.getUserId() == null ? null : new UserId(data.getUserId()), data.getOptions(),
                Boolean.TRUE.equals(data.getDummy()), data.getCreatedAt(), data.getExpiresAt(), null);
    }
}
