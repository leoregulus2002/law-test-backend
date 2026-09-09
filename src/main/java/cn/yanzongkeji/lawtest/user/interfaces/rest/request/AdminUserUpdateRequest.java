package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;

public record AdminUserUpdateRequest(String displayName, UserRole role, UserStatus status) {
}
