package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import java.util.List;
import java.util.Optional;

/** 练习和答题场景所需的轻量题目查询能力。 */
public interface QuestionPracticeQuery {

    /** 统计全部或指定题库范围内的题目数量。 */
    long countByBankIds(List<QuestionBankId> questionBankIds);

    /** 按 ID 游标查询全部或指定题库范围内的题目 ID。 */
    List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit);

    /** 在全部或指定题库范围内随机选取一个题目 ID。 */
    Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds);
}
