package cn.yanzongkeji.lawtest.question.domain.model;

public record QuestionId(long value) {
    public QuestionId {
        if (value <= 0)
            throw new IllegalArgumentException("题目 ID 必须为正整数");
    }
}
