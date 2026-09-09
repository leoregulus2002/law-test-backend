package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionAnswerMapper extends BaseMapper<QuestionAnswerDO> {

    /** 使用 PostgreSQL 关联删除指定题库的全部答案，避免展开超长 ID 参数列表。 */
    int deleteByBankId(@Param("questionBankId") long questionBankId);
}
