package cn.yanzongkeji.lawtest.user.domain.port;

import java.time.Instant;

public interface AccessTokenDenylist {
    void revoke(String token, Instant expiresAt);
    boolean isRevoked(String token);
}
