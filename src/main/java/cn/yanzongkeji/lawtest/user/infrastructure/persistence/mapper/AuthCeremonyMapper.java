package cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AuthCeremonyDO;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.type.ObjectTypeHandler;

@Mapper
public interface AuthCeremonyMapper {
    @Insert("""
            insert into auth_ceremony
                (id, challenge, ceremony_type, user_id, options, is_dummy, created_at, expires_at, consumed_at)
            values
                (#{id,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}, #{challenge}, #{ceremonyType},
                 #{userId}, cast(#{options} as jsonb), #{dummy}, #{createdAt}, #{expiresAt}, #{consumedAt})
            """)
    int insert(AuthCeremonyDO ceremony);

    /** PostgreSQL UPDATE ... RETURNING is deliberately one statement: no select-then-update race. */
    @Select("""
            update auth_ceremony
            set consumed_at = #{now}
            where id = #{id,typeHandler=org.apache.ibatis.type.ObjectTypeHandler}
              and ceremony_type = #{type}
              and consumed_at is null
              and expires_at > #{now}
            returning id, challenge, ceremony_type, user_id, options::text as options,
                      is_dummy, created_at, expires_at, consumed_at
            """)
    @Results({
            @Result(column = "id", property = "id", javaType = UUID.class, typeHandler = ObjectTypeHandler.class),
            @Result(column = "ceremony_type", property = "ceremonyType"),
            @Result(column = "user_id", property = "userId"),
            @Result(column = "is_dummy", property = "dummy"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "expires_at", property = "expiresAt"),
            @Result(column = "consumed_at", property = "consumedAt")
    })
    AuthCeremonyDO consume(@Param("id") UUID id, @Param("type") String type, @Param("now") Instant now);
}
