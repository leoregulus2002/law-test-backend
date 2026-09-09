package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

import cn.yanzongkeji.lawtest.user.domain.model.UserRole;

public record AdminUserCreateRequest(String username, String password, String displayName, UserRole role) {
    @Override
    public String toString() {
        return "AdminUserCreateRequest[password=<redacted>]";
    }
}
