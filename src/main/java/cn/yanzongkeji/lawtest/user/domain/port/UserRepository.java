package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import cn.yanzongkeji.lawtest.user.application.admin.UserPage;
import java.util.Optional;

public interface UserRepository {
    UserId save(UserAccount user);

    Optional<UserAccount> findById(UserId id);

    Optional<UserAccount> findByUsername(String username);

    UserPage findPage(int page, int size, String keyword, UserRole role, UserStatus status);

    boolean delete(UserId id);
}
