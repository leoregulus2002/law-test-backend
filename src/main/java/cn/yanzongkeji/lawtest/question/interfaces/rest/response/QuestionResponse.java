package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "解析后的题目")
public record QuestionResponse(@Schema(description = "题号", example = "51") int number,
        @Schema(description = "题干") String stem, @Schema(description = "选项") List<QuestionOptionResponse> options,
        @Schema(description = "正确选项标识", example = "[\"B\", \"C\", \"D\"]") List<String> answers,
        @Schema(description = "题目解析") String analysis) {

    public static QuestionResponse from(Question question) {
        List<QuestionOptionResponse> optionResponses = question.options().stream().map(QuestionOptionResponse::from)
                .toList();
        return new QuestionResponse(question.number().value(), question.stem(), optionResponses,
                question.answerKey().optionLabels(), question.analysis());
    }
}
