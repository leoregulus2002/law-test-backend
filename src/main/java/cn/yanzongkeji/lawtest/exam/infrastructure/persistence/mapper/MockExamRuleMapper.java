package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject.MockExamRuleDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MockExamRuleMapper extends BaseMapper<MockExamRuleDO> {}
