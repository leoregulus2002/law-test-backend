package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "新增或完整更新题目的请求")
public record QuestionUpsertRequest(int number, String stem, List<QuestionOptionRequest> options, List<String> answers,
        String analysis) {
}
