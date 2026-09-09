package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;

public record QuestionStatusUpdateRequest(QuestionStatus status) {
}
