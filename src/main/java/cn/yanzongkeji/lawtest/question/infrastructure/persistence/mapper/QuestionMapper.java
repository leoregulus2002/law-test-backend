package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QuestionMapper extends BaseMapper<QuestionDO> {

    @Update("update question set sequence_no=#{sequenceNo}, stem=#{stem}, analysis=#{analysis}, question_type=#{questionType}, updated_at=current_timestamp where id=#{id}")
    int updateContent(QuestionDO data);

    /** 删除指定题库的全部题目主表记录。 */
    @Delete("delete from question where question_bank_id=#{questionBankId}")
    int deleteByBankId(@Param("questionBankId") long questionBankId);
}
