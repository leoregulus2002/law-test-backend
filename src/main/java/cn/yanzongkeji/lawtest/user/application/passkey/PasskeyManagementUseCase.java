package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;

public interface PasskeyManagementUseCase {
    List<Passkey> list(UserId userId);

    void delete(UserId userId, String credentialId);

    record Passkey(String credentialId, String label, List<String> transports, boolean backupEligible,
            boolean backupState, Instant createdAt, Instant lastUsedAt) {
    }
}
