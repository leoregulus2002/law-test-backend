package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionAnswerDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QuestionAnswerMapper extends BaseMapper<QuestionAnswerDO> {

    /** 按选项标识读取一个题目的全部答案。 */
    @Select("select question_id, option_label from question_answer where question_id=#{questionId} order by option_label")
    List<QuestionAnswerDO> findByQuestionId(@Param("questionId") long questionId);

    /** 删除一个题目的全部答案。 */
    @Delete("delete from question_answer where question_id=#{questionId}")
    int deleteByQuestionId(@Param("questionId") long questionId);

    /** 使用 PostgreSQL 关联删除指定题库的全部答案，避免展开超长 ID 参数列表。 */
    @Delete("""
            delete from question_answer answer_data
            using question question_data
            where answer_data.question_id=question_data.id
              and question_data.question_bank_id=#{questionBankId}
            """)
    int deleteByBankId(@Param("questionBankId") long questionBankId);
}
