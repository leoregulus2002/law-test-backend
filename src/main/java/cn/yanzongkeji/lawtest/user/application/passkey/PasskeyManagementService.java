package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.PasskeyRepository;
import cn.yanzongkeji.lawtest.user.domain.port.PasskeyRepository.StoredPasskey;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PasskeyManagementService implements PasskeyManagementUseCase {
    private final PasskeyRepository passkeys;
    private final ObjectMapper objectMapper;

    public PasskeyManagementService(PasskeyRepository passkeys, ObjectMapper objectMapper) {
        this.passkeys = passkeys;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Passkey> list(UserId userId) {
        return passkeys.findByUserId(userId).stream().map(this::toPasskey).toList();
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
        passkeys.findByCredentialId(rawId).filter(passkey -> passkey.userId().equals(userId))
                .ifPresent(passkey -> passkeys.deleteByCredentialId(rawId));
    }

    private Passkey toPasskey(StoredPasskey value) {
        return new Passkey(Base64.getUrlEncoder().withoutPadding().encodeToString(value.credentialId()),
                value.label(), transports(value.transports()), value.backupEligible(), value.backupState(),
                value.createdAt(), value.lastUsedAt());
    }

    private List<String> transports(String json) {
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("保存的 Passkey transports 无效", exception);
        }
    }
}
