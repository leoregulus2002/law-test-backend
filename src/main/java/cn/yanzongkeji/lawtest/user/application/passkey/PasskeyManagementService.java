package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.UserPasskeyDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.UserPasskeyMapper;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PasskeyManagementService implements PasskeyManagementUseCase {
    private final UserPasskeyMapper passkeys;
    private final ObjectMapper objectMapper;

    public PasskeyManagementService(UserPasskeyMapper passkeys, ObjectMapper objectMapper) {
        this.passkeys = passkeys;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Passkey> list(UserId userId) {
        return passkeys.selectByUserId(userId.value()).stream().map(this::toPasskey).toList();
    }

    @Override
    @Transactional
    public void delete(UserId userId, String credentialId) {
        byte[] rawId;
        try {
            rawId = Base64.getUrlDecoder().decode(credentialId);
        } catch (IllegalArgumentException exception) {
            return; // DELETE is deliberately idempotent, including a stale malformed client ID.
        }
        UserPasskeyDO passkey = passkeys.selectByCredentialId(rawId);
        if (passkey != null && passkey.getUserId() == userId.value()) {
            passkeys.deleteByCredentialId(rawId);
        }
    }

    private Passkey toPasskey(UserPasskeyDO value) {
        return new Passkey(Base64.getUrlEncoder().withoutPadding().encodeToString(value.getCredentialId()),
                value.getLabel(), transports(value.getTransports()), Boolean.TRUE.equals(value.getBackupEligible()),
                Boolean.TRUE.equals(value.getBackupState()), value.getCreatedAt(), value.getLastUsedAt());
    }

    private List<String> transports(String json) {
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("保存的 Passkey transports 无效", exception);
        }
    }
}
