package cn.yanzongkeji.lawtest.user.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** 持久化的刷新会话只持有令牌摘要。 */
public record RefreshSession(UUID id, UserId userId, byte[] tokenHash, UUID tokenFamilyId,
        UUID previousSessionId, Instant createdAt, Instant expiresAt, Instant usedAt, Instant revokedAt) {

    public RefreshSession {
        Objects.requireNonNull(id, "会话 ID 不能为空");
        Objects.requireNonNull(userId, "用户 ID 不能为空");
        Objects.requireNonNull(tokenFamilyId, "令牌家族 ID 不能为空");
        Objects.requireNonNull(createdAt, "创建时间不能为空");
        Objects.requireNonNull(expiresAt, "过期时间不能为空");
        if (tokenHash == null || tokenHash.length != 32)
            throw new IllegalArgumentException("令牌摘要必须为 32 字节");
        if (!expiresAt.isAfter(createdAt))
            throw new IllegalArgumentException("过期时间必须晚于创建时间");
        tokenHash = tokenHash.clone();
    }

    @Override
    public byte[] tokenHash() {
        return tokenHash.clone();
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    @Override
    public String toString() {
        return "RefreshSession[id=" + id + ", userId=" + userId + "]";
    }
}
