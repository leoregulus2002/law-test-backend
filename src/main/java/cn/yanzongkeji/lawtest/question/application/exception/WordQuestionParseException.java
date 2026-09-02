package cn.yanzongkeji.lawtest.question.application.exception;

public final class WordQuestionParseException extends RuntimeException {

    public WordQuestionParseException(String message) {
        super(message);
    }

    public WordQuestionParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
