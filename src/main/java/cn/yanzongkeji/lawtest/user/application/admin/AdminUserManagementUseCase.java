package cn.yanzongkeji.lawtest.user.application.admin;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;

public interface AdminUserManagementUseCase {
    UserPage list(int page, int size, String keyword, UserRole role, UserStatus status);

    UserAccount create(String username, String password, String displayName, UserRole role);

    UserAccount update(UserId id, String displayName, UserRole role, UserStatus status, UserId operatorId);

    void resetPassword(UserId id, String password, UserId operatorId);

    void delete(UserId id, UserId operatorId);
}
