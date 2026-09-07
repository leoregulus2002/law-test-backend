package cn.yanzongkeji.lawtest.user.application.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException() {
        super("账号暂不可用");
    }
}
