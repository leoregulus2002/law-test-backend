package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Passkey 管理用的读取与删除端口，隔离应用层与持久化实现。 */
public interface PasskeyRepository {
    List<StoredPasskey> findByUserId(UserId userId);

    Optional<StoredPasskey> findByCredentialId(byte[] credentialId);

    void deleteByCredentialId(byte[] credentialId);

    record StoredPasskey(UserId userId, byte[] credentialId, String label, String transports,
            boolean backupEligible, boolean backupState, Instant createdAt, Instant lastUsedAt) {
    }
}
