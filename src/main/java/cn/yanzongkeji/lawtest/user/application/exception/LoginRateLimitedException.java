package cn.yanzongkeji.lawtest.user.application.exception;

public class LoginRateLimitedException extends RuntimeException {
    private final long retryAfterSeconds;

    public LoginRateLimitedException(long retryAfterSeconds) {
        super("登录请求过于频繁");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
