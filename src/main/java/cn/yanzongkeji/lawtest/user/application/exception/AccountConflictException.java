package cn.yanzongkeji.lawtest.user.application.exception;

public class AccountConflictException extends RuntimeException {
    public AccountConflictException() {
        super("账号已存在");
    }
}
