package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.HighFrequencyTopicDO;

public record HighFrequencyTopicResponse(long id, String title, String summary, String category) {
    public static HighFrequencyTopicResponse from(HighFrequencyTopicDO topic) {
        return new HighFrequencyTopicResponse(topic.getId(), topic.getTitle(), topic.getSummary(), topic.getCategory());
    }
}
