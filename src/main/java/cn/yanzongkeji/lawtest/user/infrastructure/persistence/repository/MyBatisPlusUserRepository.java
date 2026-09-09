package cn.yanzongkeji.lawtest.user.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.application.admin.UserPage;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AppUserDO;
import cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper.AppUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Optional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyBatisPlusUserRepository implements UserRepository {
    private final AppUserMapper mapper;

    @Override
    public UserId save(UserAccount user) {
        AppUserDO data = toDO(user);
        if (user.id() == null) {
            mapper.insert(data);
        } else if (mapper.updateById(data) != 1) {
            throw new IllegalStateException("待更新用户不存在");
        }
        return new UserId(data.getId());
    }

    @Override
    public Optional<UserAccount> findById(UserId id) {
        return Optional.ofNullable(mapper.selectById(id.value())).map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        String normalized = UserAccount.normalizeUsername(username);
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<AppUserDO>()
                .eq(AppUserDO::getUsername, normalized))).map(this::toDomain);
    }

    @Override
    public UserPage findPage(int page, int size, String keyword, UserRole role, UserStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        LambdaQueryWrapper<AppUserDO> query = filters(keyword, role, status);
        Long total = mapper.selectCount(query);
        List<UserAccount> items = mapper.selectList(filters(keyword, role, status)
                .orderByDesc(AppUserDO::getCreatedAt)
                .last("LIMIT " + safeSize + " OFFSET " + (safePage * safeSize)))
                .stream().map(this::toDomain).toList();
        return new UserPage(items, safePage, safeSize, total == null ? 0 : total);
    }

    @Override
    public boolean delete(UserId id) {
        return mapper.deleteById(id.value()) == 1;
    }

    private static LambdaQueryWrapper<AppUserDO> filters(String keyword, UserRole role, UserStatus status) {
        LambdaQueryWrapper<AppUserDO> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.strip();
            query.and(wrapper -> wrapper.like(AppUserDO::getUsername, value)
                    .or().like(AppUserDO::getDisplayName, value));
        }
        if (role != null)
            query.eq(AppUserDO::getRole, role.name());
        if (status != null)
            query.eq(AppUserDO::getStatus, status.name());
        return query;
    }

    private AppUserDO toDO(UserAccount user) {
        AppUserDO data = new AppUserDO();
        if (user.id() != null)
            data.setId(user.id().value());
        data.setUsername(user.username());
        data.setDisplayName(user.displayName());
        data.setPasswordHash(user.passwordHash());
        data.setWebauthnUserHandle(user.webauthnUserHandle());
        data.setRole(user.role().name());
        data.setStatus(user.status().name());
        data.setCreatedAt(user.createdAt());
        data.setUpdatedAt(user.updatedAt());
        return data;
    }

    private UserAccount toDomain(AppUserDO data) {
        return UserAccount.reconstitute(new UserId(data.getId()), data.getUsername(), data.getDisplayName(),
                data.getPasswordHash(), data.getWebauthnUserHandle(), UserRole.valueOf(data.getRole()),
                UserStatus.valueOf(data.getStatus()),
                data.getCreatedAt(), data.getUpdatedAt());
    }
}
