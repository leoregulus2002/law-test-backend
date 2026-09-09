package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import java.util.List;

public record QuestionBatchStatusUpdateRequest(List<Long> questionIds, QuestionStatus status) {
}
