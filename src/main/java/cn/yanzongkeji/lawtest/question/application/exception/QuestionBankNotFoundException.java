package cn.yanzongkeji.lawtest.question.application.exception;

public final class QuestionBankNotFoundException extends RuntimeException {
    public QuestionBankNotFoundException(long id) {
        super("题库不存在: " + id);
    }
}
