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

    boolean deleteById(QuestionId questionId);

    void deleteByBankId(QuestionBankId bankId);
}
