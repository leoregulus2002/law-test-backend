package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOptionDO> {
}
