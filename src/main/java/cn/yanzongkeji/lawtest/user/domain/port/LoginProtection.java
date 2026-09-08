package cn.yanzongkeji.lawtest.user.domain.port;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;

public interface LoginProtection {
    void checkAccountRateLimit(String username);
    void checkIpRateLimit(String ip);
    boolean isLocked(UserId userId);
    void recordPasswordFailure(UserId userId);
    /** 并发失败已触发锁定时返回 false，不得清除锁定。 */
    boolean clearPasswordFailures(UserId userId);
}
