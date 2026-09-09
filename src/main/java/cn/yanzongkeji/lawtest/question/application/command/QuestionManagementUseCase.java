package cn.yanzongkeji.lawtest.question.application.command;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionCommand;

import cn.yanzongkeji.lawtest.question.domain.model.*;

public interface QuestionManagementUseCase {
    Question create(long bankId, QuestionCommand command);

    Question replace(long questionId, QuestionCommand command);

    Question changeStatus(long questionId, QuestionStatus status);

    void deleteQuestion(long questionId);

    void deleteQuestionBank(long bankId);
}
