package cn.yanzongkeji.lawtest.question.application.highfrequency;

public final class HighFrequencyTopicNotFoundException extends RuntimeException {
    public HighFrequencyTopicNotFoundException(long id) {
        super("高频考点不存在: " + id);
    }
}
