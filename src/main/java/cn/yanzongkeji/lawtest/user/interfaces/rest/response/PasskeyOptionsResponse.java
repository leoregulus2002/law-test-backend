package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import cn.yanzongkeji.lawtest.user.application.passkey.PasskeyOptions;
import java.util.UUID;

public record PasskeyOptionsResponse<T>(UUID ceremonyId, T publicKey) {
    public static <T> PasskeyOptionsResponse<T> from(PasskeyOptions<T> options) {
        return new PasskeyOptionsResponse<>(options.ceremonyId(), options.publicKey());
    }
}
