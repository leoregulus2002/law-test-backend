package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "题目详情")
public record QuestionDetailResponse(long id, long questionBankId, int number, String stem,
        List<QuestionOptionResponse> options, List<String> answers, QuestionType questionType, String analysis,
        QuestionStatus status) {
    public static QuestionDetailResponse from(Question q) {
        return new QuestionDetailResponse(q.id().value(), q.questionBankId().value(), q.number().value(), q.stem(),
                q.options().stream().map(QuestionOptionResponse::from).toList(), q.answerKey().optionLabels(),
                q.type(), q.analysis(), q.status());
    }
}
