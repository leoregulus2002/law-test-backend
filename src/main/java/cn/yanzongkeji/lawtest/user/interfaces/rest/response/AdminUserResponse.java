package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import java.time.Instant;

public record AdminUserResponse(long id, String username, String displayName, UserRole role, UserStatus status,
        Instant createdAt, Instant updatedAt) {
    public static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(user.id().value(), user.username(), user.displayName(), user.role(),
                user.status(), user.createdAt(), user.updatedAt());
    }
}
