package cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionOptionDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOptionDO> {

    /** 按显示顺序读取一个题目的全部选项。 */
    @Select("select id, question_id, label, content, display_order from question_option where question_id=#{questionId} order by display_order")
    List<QuestionOptionDO> findByQuestionId(@Param("questionId") long questionId);

    /** 删除一个题目的全部选项。 */
    @Delete("delete from question_option where question_id=#{questionId}")
    int deleteByQuestionId(@Param("questionId") long questionId);

    /** 使用 PostgreSQL 关联删除指定题库的全部选项，避免展开超长 ID 参数列表。 */
    @Delete("""
            delete from question_option option_data
            using question question_data
            where option_data.question_id=question_data.id
              and question_data.question_bank_id=#{questionBankId}
            """)
    int deleteByBankId(@Param("questionBankId") long questionBankId);
}
