package cn.yanzongkeji.lawtest.question.application.exception;

public final class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(long id) {
        super("题目不存在: " + id);
    }

    /** 创建范围内无可用题目的异常。 */
    public QuestionNotFoundException() {
        super("未找到符合条件的题目");
    }
}
