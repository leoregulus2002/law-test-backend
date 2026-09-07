package cn.yanzongkeji.lawtest.user.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A short-lived, single-use WebAuthn ceremony stored outside of an HTTP session. */
public record AuthCeremony(UUID id, byte[] challenge, CeremonyType ceremonyType, UserId userId,
        String optionsJson, boolean dummy, Instant createdAt, Instant expiresAt, Instant consumedAt) {

    public AuthCeremony {
        Objects.requireNonNull(id, "ceremony ID must not be null");
        Objects.requireNonNull(ceremonyType, "ceremony type must not be null");
        Objects.requireNonNull(optionsJson, "ceremony options must not be null");
        Objects.requireNonNull(createdAt, "created time must not be null");
        Objects.requireNonNull(expiresAt, "expiry time must not be null");
        if (challenge == null || challenge.length != 32) {
            throw new IllegalArgumentException("WebAuthn challenge must be 32 bytes");
        }
        if (optionsJson.isBlank()) {
            throw new IllegalArgumentException("ceremony options must not be blank");
        }
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("ceremony expiry must be after creation");
        }
        challenge = challenge.clone();
    }

    @Override
    public byte[] challenge() {
        return challenge.clone();
    }

    public enum CeremonyType {
        REGISTER,
        AUTHENTICATE
    }
}
