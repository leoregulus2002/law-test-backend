package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import java.util.Optional;

public interface UserRepository {
    UserId save(UserAccount user);

    Optional<UserAccount> findById(UserId id);

    Optional<UserAccount> findByUsername(String username);
}
