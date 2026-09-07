package cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.RefreshSessionDO;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.type.ObjectTypeHandler;

@Mapper
public interface RefreshSessionMapper {
    // MyBatis 没有内置 UUID 类型处理器，显式使用 JDBC setObject/getObject 保留 PostgreSQL UUID 类型。
    @Insert("""
            insert into refresh_session
                (id, user_id, token_hash, token_family_id, previous_session_id,
                 created_at, expires_at, used_at, revoked_at)
            values
                (#{id,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}, #{userId}, #{tokenHash},
                 #{tokenFamilyId,typeHandler=org.apache.ibatis.type.ObjectTypeHandler},
                 #{previousSessionId,jdbcType=OTHER,typeHandler=org.apache.ibatis.type.ObjectTypeHandler},
                 #{createdAt}, #{expiresAt}, #{usedAt}, #{revokedAt})
            """)
    int insert(RefreshSessionDO session);

    // 共用根行锁防止旧令牌重用检查与后继会话轮换并发，遗漏刚插入的后继会话。
    @Select("""
            select root.id::text from refresh_session root
            where root.token_family_id = (
                select token_family_id from refresh_session where token_hash = #{tokenHash})
              and root.previous_session_id is null
            for update
            """)
    String lockFamilyByTokenHash(@Param("tokenHash") byte[] tokenHash);

    @Select("select * from refresh_session where token_hash = #{tokenHash} for update")
    @Results({
            @Result(column = "id", property = "id", javaType = UUID.class, typeHandler = ObjectTypeHandler.class),
            @Result(column = "token_family_id", property = "tokenFamilyId", javaType = UUID.class,
                    typeHandler = ObjectTypeHandler.class),
            @Result(column = "previous_session_id", property = "previousSessionId", javaType = UUID.class,
                    typeHandler = ObjectTypeHandler.class)
    })
    RefreshSessionDO selectByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);

    @Update("""
            update refresh_session set used_at = #{now}
            where id = #{id,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}
              and used_at is null and revoked_at is null and expires_at > #{now}
            """)
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);

    @Update("""
            update refresh_session set revoked_at = #{now}
            where token_family_id = #{familyId,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}
              and revoked_at is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    @Update("""
            update refresh_session set revoked_at = #{now}
            where id = #{id,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}
              and revoked_at is null and used_at is null and expires_at > #{now}
            """)
    int revokeActive(@Param("id") UUID id, @Param("now") Instant now);
}
