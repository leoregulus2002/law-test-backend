package cn.yanzongkeji.lawtest.question.domain.model;

public record QuestionBankId(long value) {
    public QuestionBankId {
        if (value <= 0)
            throw new IllegalArgumentException("题库 ID 必须为正整数");
    }
}
