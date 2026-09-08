package cn.yanzongkeji.lawtest.user.application.exception;

public class AuthStateUnavailableException extends RuntimeException {
    public AuthStateUnavailableException(Throwable cause) {
        super("认证状态服务暂不可用", cause);
    }
}
