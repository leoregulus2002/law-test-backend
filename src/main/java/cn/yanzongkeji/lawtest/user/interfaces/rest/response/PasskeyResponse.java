package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import cn.yanzongkeji.lawtest.user.application.passkey.PasskeyManagementUseCase.Passkey;
import java.time.Instant;
import java.util.List;

public record PasskeyResponse(String credentialId, String label, List<String> transports, boolean backupEligible,
        boolean backupState, Instant createdAt, Instant lastUsedAt) {
    public static PasskeyResponse from(Passkey passkey) {
        return new PasskeyResponse(passkey.credentialId(), passkey.label(), passkey.transports(),
                passkey.backupEligible(), passkey.backupState(), passkey.createdAt(), passkey.lastUsedAt());
    }
}
