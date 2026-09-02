package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionOption;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目选项")
public record QuestionOptionResponse(@Schema(description = "选项标识", example = "A") String label,
        @Schema(description = "选项内容") String content) {

    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(option.label(), option.content());
    }
}
