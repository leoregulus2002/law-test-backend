package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import java.util.List;

public record QuestionBatchDeleteRequest(List<Long> questionIds) {
}
