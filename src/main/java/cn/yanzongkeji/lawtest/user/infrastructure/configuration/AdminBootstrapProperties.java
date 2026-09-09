package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 首个管理员由部署环境提供，避免开放管理员自助注册入口。 */
@ConfigurationProperties(prefix = "law-test.admin-bootstrap")
public record AdminBootstrapProperties(String username, String password, String displayName) {
    public boolean enabled() {
        return username != null && !username.isBlank() && password != null && !password.isBlank()
                && displayName != null && !displayName.isBlank();
    }
}
