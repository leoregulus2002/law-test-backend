package cn.yanzongkeji.lawtest.question.domain.port;

import cn.yanzongkeji.lawtest.question.domain.model.Question;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import java.util.Collection;
import java.util.Optional;

/**
 * 题目聚合持久化端口，只负责聚合的加载、保存与删除。
 */
public interface QuestionRepository {

    void saveAll(QuestionBankId bankId, Collection<Question> questions);

    QuestionId save(Question question);

    Optional<Question> findById(QuestionId questionId);

    boolean updateStatus(QuestionId questionId, QuestionStatus status);

    boolean deleteById(QuestionId questionId);

    void deleteByBankId(QuestionBankId bankId);
}
