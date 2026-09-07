package cn.yanzongkeji.lawtest.user.infrastructure.webauthn;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AppUserDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.AppUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.stereotype.Repository;

/** Maps the durable app_user WebAuthn handle directly to Spring Security's user entity. */
@Repository
@RequiredArgsConstructor
public class PostgresPublicKeyCredentialUserEntityRepository implements PublicKeyCredentialUserEntityRepository {
    private final AppUserMapper users;

    @Override
    public PublicKeyCredentialUserEntity findById(Bytes id) {
        if (id == null) {
            throw new IllegalArgumentException("WebAuthn user ID must not be null");
        }
        return toUserEntity(users.selectOne(new LambdaQueryWrapper<AppUserDO>()
                .eq(AppUserDO::getWebauthnUserHandle, id.getBytes())));
    }

    @Override
    public PublicKeyCredentialUserEntity findByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("WebAuthn username must not be blank");
        }
        return toUserEntity(users.selectOne(new LambdaQueryWrapper<AppUserDO>()
                .eq(AppUserDO::getUsername, UserAccount.normalizeUsername(username))));
    }

    @Override
    public void save(PublicKeyCredentialUserEntity userEntity) {
        if (userEntity == null) {
            throw new IllegalArgumentException("WebAuthn user entity must not be null");
        }
        // app_user is the source of truth. Registration may invoke save after resolving this
        // entity, but it must never create an account or replace its stable random handle.
        AppUserDO existing = users.selectOne(new LambdaQueryWrapper<AppUserDO>()
                .eq(AppUserDO::getWebauthnUserHandle, userEntity.getId().getBytes()));
        if (existing == null || !existing.getUsername().equals(UserAccount.normalizeUsername(userEntity.getName()))
                || !Arrays.equals(existing.getWebauthnUserHandle(), userEntity.getId().getBytes())) {
            throw new IllegalArgumentException("WebAuthn user entity does not belong to an account");
        }
    }

    @Override
    public void delete(Bytes id) {
        if (id == null) {
            throw new IllegalArgumentException("WebAuthn user ID must not be null");
        }
        // app_user owns the handle and database foreign keys cascade passkeys on account deletion.
        // Deleting an entity here must not orphan or mutate an account.
    }

    private static PublicKeyCredentialUserEntity toUserEntity(AppUserDO user) {
        if (user == null) {
            return null;
        }
        return ImmutablePublicKeyCredentialUserEntity.builder()
                .id(new Bytes(user.getWebauthnUserHandle()))
                .name(user.getUsername())
                .displayName(user.getDisplayName())
                .build();
    }
}
