package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

public record RefreshTokenRequest(String refreshToken) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=<redacted>]";
    }
}
