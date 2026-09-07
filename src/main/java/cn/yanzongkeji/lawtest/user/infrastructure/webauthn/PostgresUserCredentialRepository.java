package cn.yanzongkeji.lawtest.user.infrastructure.webauthn;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AppUserDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.UserPasskeyDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.AppUserMapper;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.UserPasskeyMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialType;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** PostgreSQL-backed CredentialRecord adapter; all byte conversions stay at this API boundary. */
@Repository
@RequiredArgsConstructor
public class PostgresUserCredentialRepository implements UserCredentialRepository {
    private final UserPasskeyMapper passkeys;
    private final AppUserMapper users;
    private final ObjectMapper objectMapper;

    @Override
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        requireBytes(credentialId, "credential ID");
        return toCredential(passkeys.selectByCredentialId(credentialId.getBytes()));
    }

    @Override
    public List<CredentialRecord> findByUserId(Bytes userId) {
        requireBytes(userId, "WebAuthn user ID");
        AppUserDO user = findUserByHandle(userId);
        if (user == null) {
            return List.of();
        }
        return passkeys.selectByUserId(user.getId()).stream().map(this::toCredential).toList();
    }

    @Override
    public void save(CredentialRecord record) {
        Objects.requireNonNull(record, "credential record must not be null");
        AppUserDO user = findUserByHandle(record.getUserEntityUserId());
        if (user == null) {
            throw new IllegalArgumentException("credential record references an unknown WebAuthn user");
        }
        UserPasskeyDO data = new UserPasskeyDO();
        data.setUserId(user.getId());
        data.setCredentialType(record.getCredentialType().getValue());
        data.setCredentialId(record.getCredentialId().getBytes());
        data.setCosePublicKey(record.getPublicKey().getBytes());
        data.setSignatureCount(record.getSignatureCount());
        data.setUserVerified(record.isUvInitialized());
        data.setTransports(serializeTransports(record.getTransports()));
        data.setBackupEligible(record.isBackupEligible());
        data.setBackupState(record.isBackupState());
        // CredentialRecord does not expose an AAGUID; it remains null in this normalized schema.
        data.setAttestationObject(bytesOf(record.getAttestationObject()));
        data.setClientDataJson(bytesOf(record.getAttestationClientDataJSON()));
        data.setLabel(record.getLabel());
        data.setCreatedAt(record.getCreated());
        data.setLastUsedAt(record.getLastUsed());
        if (passkeys.upsert(data) != 1) {
            throw new IllegalStateException("WebAuthn credential persistence failed");
        }
    }

    @Override
    public void delete(Bytes credentialId) {
        requireBytes(credentialId, "credential ID");
        passkeys.deleteByCredentialId(credentialId.getBytes());
    }

    private AppUserDO findUserByHandle(Bytes userId) {
        requireBytes(userId, "WebAuthn user ID");
        return users.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AppUserDO>()
                .eq(AppUserDO::getWebauthnUserHandle, userId.getBytes()));
    }

    private CredentialRecord toCredential(UserPasskeyDO data) {
        if (data == null) {
            return null;
        }
        AppUserDO user = users.selectById(data.getUserId());
        if (user == null) {
            throw new IllegalStateException("credential references a missing account");
        }
        return ImmutableCredentialRecord.builder()
                .credentialType(PublicKeyCredentialType.valueOf(data.getCredentialType()))
                .credentialId(new Bytes(data.getCredentialId()))
                .publicKey(new ImmutablePublicKeyCose(data.getCosePublicKey()))
                .signatureCount(data.getSignatureCount())
                .uvInitialized(Boolean.TRUE.equals(data.getUserVerified()))
                .transports(deserializeTransports(data.getTransports()))
                .backupEligible(Boolean.TRUE.equals(data.getBackupEligible()))
                .backupState(Boolean.TRUE.equals(data.getBackupState()))
                .userEntityUserId(new Bytes(user.getWebauthnUserHandle()))
                .attestationObject(bytesOrNull(data.getAttestationObject()))
                .attestationClientDataJSON(bytesOrNull(data.getClientDataJson()))
                .label(data.getLabel())
                .created(data.getCreatedAt())
                .lastUsed(data.getLastUsedAt())
                .build();
    }

    private String serializeTransports(Set<AuthenticatorTransport> transports) {
        try {
            return objectMapper.writeValueAsString(transports.stream().map(AuthenticatorTransport::getValue)
                    .collect(Collectors.toCollection(TreeSet::new)));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("unable to serialize authenticator transports", exception);
        }
    }

    private Set<AuthenticatorTransport> deserializeTransports(String transportsJson) {
        try {
            String[] transports = objectMapper.readValue(transportsJson, String[].class);
            return Arrays.stream(transports).map(AuthenticatorTransport::valueOf).collect(Collectors.toUnmodifiableSet());
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new IllegalStateException("stored authenticator transports are invalid", exception);
        }
    }

    private static Bytes bytesOrNull(byte[] value) {
        return value == null ? null : new Bytes(value);
    }

    private static byte[] bytesOf(Bytes value) {
        return value == null ? null : value.getBytes();
    }

    private static void requireBytes(Bytes value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }
}
