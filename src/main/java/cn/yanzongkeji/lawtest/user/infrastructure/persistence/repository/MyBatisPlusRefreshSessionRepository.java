package cn.yanzongkeji.lawtest.user.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.user.domain.model.RefreshSession;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.RefreshSessionRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.RefreshSessionDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.RefreshSessionMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisPlusRefreshSessionRepository implements RefreshSessionRepository {
    private final RefreshSessionMapper mapper;

    @Override
    public void insert(RefreshSession session) {
        RefreshSessionDO data = new RefreshSessionDO();
        data.setId(session.id());
        data.setUserId(session.userId().value());
        data.setTokenHash(session.tokenHash());
        data.setTokenFamilyId(session.tokenFamilyId());
        data.setPreviousSessionId(session.previousSessionId());
        data.setCreatedAt(session.createdAt());
        data.setExpiresAt(session.expiresAt());
        data.setUsedAt(session.usedAt());
        data.setRevokedAt(session.revokedAt());
        if (mapper.insert(data) != 1)
            throw new IllegalStateException("刷新会话创建失败");
    }

    @Override
    public Optional<RefreshSession> findByTokenHashForUpdate(byte[] tokenHash) {
        if (mapper.lockFamilyByTokenHash(tokenHash) == null)
            return Optional.empty();
        return Optional.ofNullable(mapper.selectByTokenHashForUpdate(tokenHash))
                .map(data -> new RefreshSession(data.getId(), new UserId(data.getUserId()), data.getTokenHash(),
                        data.getTokenFamilyId(), data.getPreviousSessionId(), data.getCreatedAt(),
                        data.getExpiresAt(), data.getUsedAt(), data.getRevokedAt()));
    }

    @Override
    public void markUsed(UUID sessionId, Instant now) {
        if (mapper.markUsed(sessionId, now) != 1)
            throw new IllegalStateException("刷新会话状态已变更");
    }

    @Override
    public void revokeFamily(UUID tokenFamilyId, Instant now) {
        mapper.revokeFamily(tokenFamilyId, now);
    }

    @Override
    public void revokeActive(UUID sessionId, Instant now) {
        mapper.revokeActive(sessionId, now);
    }
}
