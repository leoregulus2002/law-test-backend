package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import cn.yanzongkeji.lawtest.user.application.auth.TokenPair;

public record TokenPairResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    public static TokenPairResponse from(TokenPair tokens) {
        return new TokenPairResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer", tokens.expiresIn());
    }

    @Override
    public String toString() {
        return "TokenPairResponse[accessToken=<redacted>, refreshToken=<redacted>, tokenType="
                + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
