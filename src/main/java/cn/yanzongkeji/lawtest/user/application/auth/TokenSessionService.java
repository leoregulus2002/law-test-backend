package cn.yanzongkeji.lawtest.user.application.auth;

import cn.yanzongkeji.lawtest.user.domain.model.RefreshSession;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import cn.yanzongkeji.lawtest.user.domain.port.AccessTokenIssuer;
import cn.yanzongkeji.lawtest.user.domain.port.RefreshSessionRepository;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenSessionService {
    private final RefreshSessionRepository sessions;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokens;
    private final AuthProperties.Jwt properties;
    private final SecureRandom random = new SecureRandom();

    public TokenSessionService(RefreshSessionRepository sessions, UserRepository users,
            AccessTokenIssuer accessTokens, AuthProperties properties) {
        this.sessions = sessions;
        this.users = users;
        this.accessTokens = accessTokens;
        this.properties = properties.jwt();
        if (this.properties.refreshTokenTtl().isNegative() || this.properties.refreshTokenTtl().isZero())
            throw new IllegalArgumentException("Refresh Token 有效期必须为正数");
    }

    @Transactional
    public TokenPair issue(UserAccount user) {
        Objects.requireNonNull(user.id(), "签发令牌前必须保存用户");
        if (user.status() != UserStatus.ACTIVE)
            throw new InvalidRefreshTokenException();
        return createPair(user, UUID.randomUUID(), null, Instant.now());
    }

    // 重用导致的撤销必须提交，不能随对外的认证失败异常一起回滚。
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public TokenPair refresh(String refreshToken) {
        RefreshSession current = sessions.findByTokenHashForUpdate(hash(requireToken(refreshToken)))
                .orElseThrow(InvalidRefreshTokenException::new);
        Instant now = Instant.now();
        if (current.usedAt() != null || current.revokedAt() != null) {
            sessions.revokeFamily(current.tokenFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (current.isExpired(now))
            throw new InvalidRefreshTokenException();
        UserAccount user = users.findById(current.userId())
                .filter(account -> account.status() == UserStatus.ACTIVE)
                .orElseThrow(InvalidRefreshTokenException::new);
        sessions.markUsed(current.id(), now);
        return createPair(user, current.tokenFamilyId(), current.id(), now);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!isTokenFormatValid(refreshToken))
            return;
        sessions.findByTokenHashForUpdate(hash(refreshToken))
                .ifPresent(session -> sessions.revokeActive(session.id(), Instant.now()));
    }

    private TokenPair createPair(UserAccount user, UUID familyId, UUID previousSessionId, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String accessToken = accessTokens.issue(user, now);
        sessions.insert(new RefreshSession(UUID.randomUUID(), user.id(), hash(refreshToken), familyId,
                previousSessionId, now, now.plus(properties.refreshTokenTtl()), null, null));
        return new TokenPair(accessToken, refreshToken, properties.accessTokenTtl().toSeconds());
    }

    private static String requireToken(String token) {
        if (!isTokenFormatValid(token))
            throw new InvalidRefreshTokenException();
        return token;
    }

    private static boolean isTokenFormatValid(String token) {
        return token != null && token.length() == 43 && token.matches("[A-Za-z0-9_-]{43}");
    }

    private static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
