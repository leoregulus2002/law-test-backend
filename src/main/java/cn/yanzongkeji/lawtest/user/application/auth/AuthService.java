package cn.yanzongkeji.lawtest.user.application.auth;

import cn.yanzongkeji.lawtest.user.application.exception.AccountConflictException;
import cn.yanzongkeji.lawtest.user.application.exception.AccountLockedException;
import cn.yanzongkeji.lawtest.user.application.exception.AuthenticationFailedException;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import cn.yanzongkeji.lawtest.user.domain.port.LoginProtection;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthUseCase {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokens;
    private final LoginProtection protection;
    private final SecureRandom random = new SecureRandom();
    private final String missingAccountHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenSessionService tokens,
            LoginProtection protection) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.protection = protection;
        // 与真实密码使用相同 BCrypt 成本，每个服务实例仅计算一次。
        this.missingAccountHash = passwordEncoder.encode("missing-account-timing-placeholder");
    }

    @Override
    @Transactional
    public TokenPair register(String username, String password, String displayName) {
        validatePasswordForRegistration(password);
        String normalized = UserAccount.normalizeUsername(username);
        byte[] userHandle = new byte[32];
        random.nextBytes(userHandle);
        UserAccount account = UserAccount.register(normalized, displayName,
                passwordEncoder.encode(password), userHandle, Instant.now());
        UserId id;
        try {
            id = users.save(account);
        } catch (DuplicateKeyException exception) {
            // 只将用户名唯一约束映射为账号冲突，不能掩盖其他数据库问题。
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause.getMessage() != null && cause.getMessage().contains("app_user_username_unique"))
                    throw new AccountConflictException();
            }
            throw exception;
        }
        UserAccount savedUser = users.findById(id)
                .orElseThrow(() -> new IllegalStateException("保存后的用户不存在"));
        return tokens.issue(savedUser);
    }

    @Override
    public TokenPair passwordLogin(String username, String password) {
        protection.checkAccountRateLimit(username);
        UserAccount user = null;
        try {
            user = users.findByUsername(UserAccount.normalizeUsername(username)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            // 非法账号也执行 BCrypt 校验并返回相同认证失败响应。
        }
        boolean validPassword = isPasswordValid(password);
        boolean matches = passwordEncoder.matches(validPassword ? password : "invalid-password-placeholder",
                user != null && validPassword ? user.passwordHash() : missingAccountHash);
        if (user == null)
            throw new AuthenticationFailedException();
        if (user.status() != UserStatus.ACTIVE || protection.isLocked(user.id()))
            throw new AccountLockedException();
        if (!validPassword || !matches) {
            protection.recordPasswordFailure(user.id());
            throw new AuthenticationFailedException();
        }
        if (!protection.clearPasswordFailures(user.id()))
            throw new AccountLockedException();
        return tokens.issue(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        // Redis 脚本原子执行轮换及重放撤销，不参与数据库事务。
        return tokens.refresh(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        tokens.logout(refreshToken);
    }

    private static void validatePasswordForRegistration(String password) {
        if (password == null)
            throw new IllegalArgumentException("密码不能为空");
        int characters = password.codePointCount(0, password.length());
        if (characters < 8 || characters > 72)
            throw new IllegalArgumentException("密码必须为 8–72 个字符");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("密码的 UTF-8 编码不能超过 72 字节");
    }

    private static boolean isPasswordValid(String password) {
        if (password == null)
            return false;
        int characters = password.codePointCount(0, password.length());
        return characters >= 8 && characters <= 72
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
