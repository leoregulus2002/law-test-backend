package cn.yanzongkeji.lawtest.user.application.auth;

/** 仅用于认证成功响应，不得记录令牌明文。expiresIn 的单位为秒。 */
public record TokenPair(String accessToken, String refreshToken, long expiresIn) {
    @Override
    public String toString() {
        return "TokenPair[accessToken=<redacted>, refreshToken=<redacted>, expiresIn=" + expiresIn + "]";
    }
}
