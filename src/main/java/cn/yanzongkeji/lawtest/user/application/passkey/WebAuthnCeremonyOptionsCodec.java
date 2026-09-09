package cn.yanzongkeji.lawtest.user.application.passkey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

/**
 * Encodes the Spring Security WebAuthn option objects stored in short-lived ceremony records.
 *
 * <p>Those option objects are serializable but deliberately do not expose Jackson creators, so
 * JSON cannot be used to restore them.</p>
 */
final class WebAuthnCeremonyOptionsCodec {
    private static final int MAX_ENCODED_LENGTH = 65_536;
    private static final ObjectInputFilter INPUT_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=32;maxrefs=256;maxbytes=49152;java.base/*;org.springframework.security.web.webauthn.api.*;!*");

    String write(Serializable options) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(options);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存 WebAuthn ceremony", exception);
        }
    }

    <T> T read(String encoded, Class<T> type) {
        if (encoded.length() > MAX_ENCODED_LENGTH) {
            throw new IllegalStateException("WebAuthn ceremony 数据损坏");
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
                input.setObjectInputFilter(INPUT_FILTER);
                return type.cast(input.readObject());
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException | ClassCastException exception) {
            throw new IllegalStateException("WebAuthn ceremony 数据损坏", exception);
        }
    }
}
