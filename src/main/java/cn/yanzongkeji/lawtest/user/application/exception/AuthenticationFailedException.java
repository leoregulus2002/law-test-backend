package cn.yanzongkeji.lawtest.user.application.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("账号或凭证无效");
    }
}
