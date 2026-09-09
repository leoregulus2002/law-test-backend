package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

public record AdminPasswordResetRequest(String password) {
    @Override
    public String toString() {
        return "AdminPasswordResetRequest[password=<redacted>]";
    }
}
