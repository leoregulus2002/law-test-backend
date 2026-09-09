package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOptionDO> {

    /** 使用 PostgreSQL 关联删除指定题库的全部选项，避免展开超长 ID 参数列表。 */
    int deleteByBankId(@Param("questionBankId") long questionBankId);
}
