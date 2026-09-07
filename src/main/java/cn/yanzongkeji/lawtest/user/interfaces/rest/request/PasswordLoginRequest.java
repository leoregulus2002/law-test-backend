package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

public record PasswordLoginRequest(String username, String password) {
    @Override
    public String toString() {
        return "PasswordLoginRequest[password=<redacted>]";
    }
}
