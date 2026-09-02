package cn.yanzongkeji.lawtest.question.application.query;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;

import cn.yanzongkeji.lawtest.question.domain.model.*;

public interface QuestionQueryUseCase {
    QuestionPage<QuestionBank> questionBanks(int page, int size);

    QuestionBank questionBank(long id);

    QuestionPage<Question> questions(long bankId, int page, int size);

    Question question(long id);
}
