package cn.yanzongkeji.lawtest.user.application.exception;

/** A credential ID or a passkey label is already in use for this account. */
public final class PasskeyConflictException extends RuntimeException {
    public PasskeyConflictException() {
        super("Passkey already exists");
    }
}
