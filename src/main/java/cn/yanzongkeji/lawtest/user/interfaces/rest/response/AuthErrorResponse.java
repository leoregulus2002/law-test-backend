package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import java.time.Instant;

public record AuthErrorResponse(String code, String message, Instant timestamp) {
    public static AuthErrorResponse of(String code, String message) {
        return new AuthErrorResponse(code, message, Instant.now());
    }
}
