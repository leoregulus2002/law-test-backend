package cn.yanzongkeji.lawtest.user.interfaces.rest.response;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import java.time.Instant;

public record CurrentUserResponse(long id, String username, String displayName, UserStatus status, Instant createdAt) {
    public static CurrentUserResponse from(UserAccount user) {
        return new CurrentUserResponse(user.id().value(), user.username(), user.displayName(), user.status(), user.createdAt());
    }
}
