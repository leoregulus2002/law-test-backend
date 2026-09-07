package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.AuthCeremony;
import java.time.Instant;
import java.util.UUID;

public interface AuthCeremonyRepository {
    void create(AuthCeremony ceremony);

    /**
     * Atomically consumes and returns the original ceremony. A ceremony can only be returned
     * once, and only before its expiry.
     */
    AuthCeremony consume(UUID id, AuthCeremony.CeremonyType type, Instant now);
}
