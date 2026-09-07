package cn.yanzongkeji.lawtest.user.application.auth;

/** 对外统一为 401；不能泄露令牌不存在、过期、撤销或重用等内部原因。 */
public final class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("认证令牌无效");
    }
}
