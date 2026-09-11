package cn.yanzongkeji.lawtest.exam.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject.MockExamBankDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MockExamBankMapper extends BaseMapper<MockExamBankDO> {}
