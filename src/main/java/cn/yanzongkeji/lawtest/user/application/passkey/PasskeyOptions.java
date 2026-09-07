package cn.yanzongkeji.lawtest.user.application.passkey;

import java.util.UUID;

/** Browser/WebAuthn options paired with their durable, single-use ceremony ID. */
public record PasskeyOptions<T>(UUID ceremonyId, T publicKey) {
}
