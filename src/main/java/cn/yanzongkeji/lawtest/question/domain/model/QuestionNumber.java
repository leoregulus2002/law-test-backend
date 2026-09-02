package cn.yanzongkeji.lawtest.question.domain.model;

/**
 * 题号值对象。
 */
public record QuestionNumber(int value) {

    public QuestionNumber {
        if (value <= 0) {
            throw new IllegalArgumentException("题号必须是正整数");
        }
    }
}
