package cn.yanzongkeji.lawtest.user.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.PasskeyRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.UserPasskeyDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.UserPasskeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** MyBatis-Plus 对 Passkey 管理端口的适配器。 */
@Repository
@RequiredArgsConstructor
public class MyBatisPlusPasskeyRepository implements PasskeyRepository {
    private final UserPasskeyMapper mapper;

    @Override
    public List<StoredPasskey> findByUserId(UserId userId) {
        return mapper.selectList(new LambdaQueryWrapper<UserPasskeyDO>()
                .eq(UserPasskeyDO::getUserId, userId.value()).orderByAsc(UserPasskeyDO::getId))
                .stream().map(MyBatisPlusPasskeyRepository::toStored).toList();
    }

    @Override
    public Optional<StoredPasskey> findByCredentialId(byte[] credentialId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<UserPasskeyDO>()
                .eq(UserPasskeyDO::getCredentialId, credentialId))).map(MyBatisPlusPasskeyRepository::toStored);
    }

    @Override
    public void deleteByCredentialId(byte[] credentialId) {
        mapper.delete(new LambdaQueryWrapper<UserPasskeyDO>().eq(UserPasskeyDO::getCredentialId, credentialId));
    }

    private static StoredPasskey toStored(UserPasskeyDO data) {
        return new StoredPasskey(new UserId(data.getUserId()), data.getCredentialId(), data.getLabel(),
                data.getTransports(), Boolean.TRUE.equals(data.getBackupEligible()), Boolean.TRUE.equals(data.getBackupState()),
                data.getCreatedAt(), data.getLastUsedAt());
    }
}
