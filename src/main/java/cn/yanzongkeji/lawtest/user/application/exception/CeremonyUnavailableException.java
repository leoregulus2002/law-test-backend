package cn.yanzongkeji.lawtest.user.application.exception;

/** Raised when a WebAuthn ceremony is missing, expired, or has already been consumed. */
public class CeremonyUnavailableException extends RuntimeException {
    public CeremonyUnavailableException() {
        super("WebAuthn ceremony is unavailable");
    }
}
