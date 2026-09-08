package cn.yanzongkeji.lawtest.user.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** 用户账户聚合；接口层应使用专用响应对象，不能直接返回包含密码散列的聚合。 */
public final class UserAccount {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[a-z0-9._-]{4,64}");

    private final UserId id;
    private final String username;
    private final String displayName;
    private final String passwordHash;
    private final byte[] webauthnUserHandle;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private UserAccount(UserId id, String username, String displayName, String passwordHash,
            byte[] webauthnUserHandle, UserStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = normalizeUsername(username);
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("显示名称不能为空");
        this.displayName = displayName.strip();
        if (this.displayName.codePointCount(0, this.displayName.length()) > 64)
            throw new IllegalArgumentException("显示名称不能超过 64 个字符");
        if (passwordHash == null || passwordHash.isBlank() || passwordHash.length() > 255)
            throw new IllegalArgumentException("密码散列不能为空且不能超过 255 个字符");
        this.passwordHash = passwordHash;
        if (webauthnUserHandle == null || webauthnUserHandle.length != 32)
            throw new IllegalArgumentException("WebAuthn 用户标识必须为 32 字节");
        this.webauthnUserHandle = webauthnUserHandle.clone();
        this.status = Objects.requireNonNull(status, "用户状态不能为空");
        this.createdAt = Objects.requireNonNull(createdAt, "创建时间不能为空");
        this.updatedAt = Objects.requireNonNull(updatedAt, "更新时间不能为空");
    }

    public static UserAccount register(String username, String displayName, String passwordHash,
            byte[] webauthnUserHandle, Instant now) {
        return new UserAccount(null, username, displayName, passwordHash, webauthnUserHandle,
                UserStatus.ACTIVE, now, now);
    }

    public static UserAccount reconstitute(UserId id, String username, String displayName,
            String passwordHash, byte[] webauthnUserHandle, UserStatus status, Instant createdAt, Instant updatedAt) {
        return new UserAccount(Objects.requireNonNull(id, "用户 ID 不能为空"), username, displayName,
                passwordHash, webauthnUserHandle, status, createdAt, updatedAt);
    }

    public static String normalizeUsername(String username) {
        if (username == null)
            throw new IllegalArgumentException("用户名不能为空");
        String normalized = username.strip().toLowerCase(Locale.ROOT);
        if (!USERNAME_PATTERN.matcher(normalized).matches())
            throw new IllegalArgumentException("用户名必须为 4–64 位字母、数字、点、下划线或连字符");
        return normalized;
    }

    public UserId id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    /** 仅供应用认证与持久化使用，不得映射到 REST 响应。 */
    public String passwordHash() {
        return passwordHash;
    }

    public byte[] webauthnUserHandle() {
        return webauthnUserHandle.clone();
    }

    public UserStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
