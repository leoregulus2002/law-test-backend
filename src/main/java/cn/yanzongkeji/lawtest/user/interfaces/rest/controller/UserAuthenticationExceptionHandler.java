package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.application.auth.InvalidRefreshTokenException;
import cn.yanzongkeji.lawtest.user.application.exception.AccountConflictException;
import cn.yanzongkeji.lawtest.user.application.exception.AccountLockedException;
import cn.yanzongkeji.lawtest.user.application.exception.AuthenticationFailedException;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.AuthErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "cn.yanzongkeji.lawtest.user.interfaces.rest.controller")
public class UserAuthenticationExceptionHandler {
    @ExceptionHandler({AuthenticationFailedException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<AuthErrorResponse> authenticationFailed(RuntimeException exception) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "账号或凭证无效");
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<AuthErrorResponse> accountUnavailable(AccountLockedException exception) {
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "账号暂不可用");
    }

    @ExceptionHandler(AccountConflictException.class)
    public ResponseEntity<AuthErrorResponse> accountConflict(AccountConflictException exception) {
        return error(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", "账号已存在");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthErrorResponse> validationFailed(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<AuthErrorResponse> malformedRequest(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "请求 JSON 格式或字段类型无效");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthErrorResponse> unexpectedFailure(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "服务器内部错误");
    }

    private ResponseEntity<AuthErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(AuthErrorResponse.of(code, message));
    }
}
