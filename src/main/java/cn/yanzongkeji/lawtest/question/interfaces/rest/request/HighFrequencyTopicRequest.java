package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "高频考点录入内容")
public record HighFrequencyTopicRequest(String title, String summary, String category) {
}
