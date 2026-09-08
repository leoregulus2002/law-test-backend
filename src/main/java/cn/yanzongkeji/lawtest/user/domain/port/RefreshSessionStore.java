package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.RefreshSession;
import java.util.Optional;

public interface RefreshSessionStore {
    void create(RefreshSession session);
    Optional<RefreshSession> find(byte[] tokenHash);
    /** 原子轮换；重放会同时删除整个家族，返回 false。 */
    boolean rotate(RefreshSession current, RefreshSession replacement);
    void revokeFamily(byte[] tokenHash);
}
