package cn.yanzongkeji.lawtest.question.application.command;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionCommand;
import cn.yanzongkeji.lawtest.question.domain.model.*;
import java.util.List;

public interface QuestionManagementUseCase {
  QuestionBank createQuestionBank(String name);

  Question create(long bankId, QuestionCommand command);

  Question replace(long questionId, QuestionCommand command);

  Question changeStatus(long questionId, QuestionStatus status);

  void changeStatuses(List<Long> questionIds, QuestionStatus status);

  void deleteQuestion(long questionId);

  void deleteQuestions(List<Long> questionIds);

  void deleteQuestionBank(long bankId);
}
