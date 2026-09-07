package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

public record RegisterRequest(String username, String password, String displayName) {
    @Override
    public String toString() {
        return "RegisterRequest[password=<redacted>]";
    }
}
