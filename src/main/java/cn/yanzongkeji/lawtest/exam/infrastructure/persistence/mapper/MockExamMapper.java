package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject.MockExamDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MockExamMapper extends BaseMapper<MockExamDO> {}
