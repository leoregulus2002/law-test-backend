package cn.yanzongkeji.lawtest.question.domain.port;

import cn.yanzongkeji.lawtest.question.domain.model.Question;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 题目持久化端口。当前版本不提供基础设施实现。
 */
public interface QuestionRepository {

    void saveAll(QuestionBankId bankId, Collection<Question> questions);

    QuestionId save(Question question);

    Optional<Question> findById(QuestionId questionId);

    List<Question> findPageByBankId(QuestionBankId bankId, int offset, int limit);

    long countByBankId(QuestionBankId bankId);

    /** 统计全部或指定题库范围内的题目数量。 */
    default long countByBankIds(List<QuestionBankId> questionBankIds) {
        return findIdsAfter(questionBankIds, null, Integer.MAX_VALUE).size();
    }

    /** 按 ID 游标查询全部或指定题库范围内的题目 ID。 */
    List<QuestionId> findIdsAfter(List<QuestionBankId> questionBankIds, Long cursor, int limit);

    /** 在全部或指定题库范围内随机选取一个题目 ID。 */
    Optional<QuestionId> findRandomId(List<QuestionBankId> questionBankIds);

    boolean deleteById(QuestionId questionId);

    void deleteByBankId(QuestionBankId bankId);
}
