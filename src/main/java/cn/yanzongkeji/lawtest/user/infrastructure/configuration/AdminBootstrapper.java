package cn.yanzongkeji.lawtest.user.infrastructure.configuration;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapper implements ApplicationRunner {
    private final AdminBootstrapProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    public AdminBootstrapper(AdminBootstrapProperties properties, UserRepository users, PasswordEncoder passwords) {
        this.properties = properties;
        this.users = users;
        this.passwords = passwords;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled())
            return;
        users.findByUsername(properties.username()).ifPresentOrElse(existing -> {
            if (existing.role() != UserRole.ADMIN)
                throw new IllegalStateException("初始管理员用户名已被普通用户占用");
        }, () -> {
            byte[] handle = new byte[32];
            random.nextBytes(handle);
            users.save(UserAccount.create(properties.username(), properties.displayName(),
                    passwords.encode(properties.password()), handle, UserRole.ADMIN, Instant.now()));
        });
    }
}
