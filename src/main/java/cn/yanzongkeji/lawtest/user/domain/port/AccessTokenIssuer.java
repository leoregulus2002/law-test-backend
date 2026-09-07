package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import java.time.Instant;

public interface AccessTokenIssuer {
    String issue(UserAccount user, Instant issuedAt);
}
