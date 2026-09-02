package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionBank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题库信息")
public record QuestionBankResponse(long id, String code, String name, String sourceFileName) {
    public static QuestionBankResponse from(QuestionBank b) {
        return new QuestionBankResponse(b.id().value(), b.code().value(), b.name(), b.sourceFileName());
    }
}
