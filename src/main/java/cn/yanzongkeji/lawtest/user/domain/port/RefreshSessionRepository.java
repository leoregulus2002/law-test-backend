package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.RefreshSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {
    void insert(RefreshSession session);

    /** 必须在事务中调用；先锁定家族根会话，再锁定匹配的会话。 */
    Optional<RefreshSession> findByTokenHashForUpdate(byte[] tokenHash);

    void markUsed(UUID sessionId, Instant now);

    void revokeFamily(UUID tokenFamilyId, Instant now);

    void revokeActive(UUID sessionId, Instant now);
}
