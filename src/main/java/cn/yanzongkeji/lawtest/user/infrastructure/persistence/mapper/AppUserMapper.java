package cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AppUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUserDO> {
    @Update("""
            update app_user
            set failed_login_attempts = failed_login_attempts + 1,
                locked_until = case when failed_login_attempts + 1 >= 5
                    then cast(#{now} as timestamp with time zone) + interval '15 minutes'
                    else locked_until end,
                updated_at = #{now}
            where id = #{userId}
            """)
    int recordPasswordFailure(@Param("userId") long userId, @Param("now") Instant now);

    @Update("""
            update app_user
            set failed_login_attempts = 0,
                locked_until = null,
                updated_at = current_timestamp
            where id = #{userId}
            """)
    int clearPasswordFailures(@Param("userId") long userId);
}
