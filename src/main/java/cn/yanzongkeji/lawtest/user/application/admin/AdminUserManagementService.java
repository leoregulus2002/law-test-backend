package cn.yanzongkeji.lawtest.user.application.admin;

import cn.yanzongkeji.lawtest.user.application.exception.AccountConflictException;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementService implements AdminUserManagementUseCase {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public AdminUserManagementService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserPage list(int page, int size, String keyword, UserRole role, UserStatus status) {
        return users.findPage(page, size, keyword, role, status);
    }

    @Override
    @Transactional
    public UserAccount create(String username, String password, String displayName, UserRole role) {
        validatePassword(password);
        byte[] handle = new byte[32];
        random.nextBytes(handle);
        UserAccount account = UserAccount.create(username, displayName, passwordEncoder.encode(password), handle,
                role == null ? UserRole.USER : role, Instant.now());
        try {
            UserId id = users.save(account);
            return users.findById(id).orElseThrow(() -> new IllegalStateException("保存后的用户不存在"));
        } catch (DuplicateKeyException exception) {
            throw new AccountConflictException();
        }
    }

    @Override
    @Transactional
    public UserAccount update(UserId id, String displayName, UserRole role, UserStatus status, UserId operatorId) {
        UserAccount current = find(id);
        if (id.equals(operatorId) && (role != current.role() || status != UserStatus.ACTIVE))
            throw new IllegalArgumentException("不能修改当前管理员的角色或禁用当前账号");
        UserAccount updated = current.updateProfile(displayName == null ? current.displayName() : displayName,
                role == null ? current.role() : role, status == null ? current.status() : status, Instant.now());
        users.save(updated);
        return updated;
    }

    @Override
    @Transactional
    public void resetPassword(UserId id, String password, UserId operatorId) {
        if (id.equals(operatorId))
            throw new IllegalArgumentException("请通过个人账号安全设置修改当前管理员密码");
        validatePassword(password);
        UserAccount user = find(id).resetPassword(passwordEncoder.encode(password), Instant.now());
        users.save(user);
    }

    @Override
    @Transactional
    public void delete(UserId id, UserId operatorId) {
        if (id.equals(operatorId))
            throw new IllegalArgumentException("不能删除当前管理员账号");
        if (!users.delete(id))
            throw new IllegalArgumentException("用户不存在");
    }

    private UserAccount find(UserId id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private static void validatePassword(String password) {
        if (password == null)
            throw new IllegalArgumentException("密码不能为空");
        int characters = password.codePointCount(0, password.length());
        if (characters < 8 || characters > 72 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("密码必须为 8–72 个字符，且 UTF-8 编码不超过 72 字节");
    }
}
