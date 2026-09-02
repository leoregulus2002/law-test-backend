package cn.yanzongkeji.lawtest.question.application.exception;

public final class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(long id) {
        super("题目不存在: " + id);
    }
}
