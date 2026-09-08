package cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.AppUserDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUserDO> {
}
