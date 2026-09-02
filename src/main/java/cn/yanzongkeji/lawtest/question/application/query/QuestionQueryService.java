package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;
import cn.yanzongkeji.lawtest.question.application.exception.*;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class QuestionQueryService implements QuestionQueryUseCase {
    private final QuestionBankRepository banks;
    private final QuestionRepository questions;

    public QuestionPage<QuestionBank> questionBanks(int page, int size) {
        int[] p = page(page, size);
        return new QuestionPage<>(banks.findPage(p[0], p[1]), page, p[1], banks.count());
    }

    public QuestionBank questionBank(long id) {
        return banks.findById(new QuestionBankId(id)).orElseThrow(() -> new QuestionBankNotFoundException(id));
    }

    public QuestionPage<Question> questions(long bankId, int page, int size) {
        QuestionBankId id = new QuestionBankId(bankId);
        if (banks.findById(id).isEmpty())
            throw new QuestionBankNotFoundException(bankId);
        int[] p = page(page, size);
        return new QuestionPage<>(questions.findPageByBankId(id, p[0], p[1]), page, p[1], questions.countByBankId(id));
    }

    public Question question(long id) {
        return questions.findById(new QuestionId(id)).orElseThrow(() -> new QuestionNotFoundException(id));
    }

    private static int[] page(int page, int size) {
        if (page < 0)
            throw new QuestionValidationException("page 不能小于 0");
        int actual = size == 0 ? 20 : size;
        if (actual < 1 || actual > 100)
            throw new QuestionValidationException("size 必须在 1 到 100 之间");
        return new int[] { Math.multiplyExact(page, actual), actual };
    }
}
