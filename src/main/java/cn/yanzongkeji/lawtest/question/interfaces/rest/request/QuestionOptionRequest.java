package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目选项写入参数")
public record QuestionOptionRequest(String label, String content) {
}
