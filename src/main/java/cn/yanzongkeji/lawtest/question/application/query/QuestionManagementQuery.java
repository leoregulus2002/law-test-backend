package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionListItem;
import java.util.List;

/** 题目管理场景所需的分页读取能力。 */
public interface QuestionManagementQuery {

    /** 跨题库分页读取管理端题目列表。 */
    List<QuestionListItem> findPage(int offset, int limit);

    long count();

    /** 按题库分页加载完整题目，结果按题号和题目 ID 升序排列。 */
    List<Question> findPageByBankId(QuestionBankId bankId, int offset, int limit);

    /** 统计指定题库的题目数量。 */
    long countByBankId(QuestionBankId bankId);
}
