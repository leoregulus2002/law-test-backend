package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.application.dto.QuestionListItem;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;

public record QuestionListResponse(long id, long questionBankId, String questionBankName, int number, String stem,
        QuestionType questionType, QuestionStatus status) {
    public static QuestionListResponse from(QuestionListItem item) {
        return new QuestionListResponse(item.id(), item.questionBankId(), item.questionBankName(), item.number(),
                item.stem(), item.questionType(), item.status());
    }
}
