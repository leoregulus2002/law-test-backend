package cn.yanzongkeji.lawtest.question.domain.port;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionBank;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionBankId;
import java.util.List;
import java.util.Optional;

public interface QuestionBankRepository {
    ImportResult createIfAbsent(QuestionBank questionBank);

    Optional<QuestionBank> findById(QuestionBankId id);

    List<QuestionBank> findPage(int offset, int limit);

    long count();

    boolean deleteById(QuestionBankId id);

    record ImportResult(QuestionBankId questionBankId, boolean imported) {
    }
}
