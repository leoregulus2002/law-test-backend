package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 单个题目 ID 响应。 */
@Schema(description = "题目 ID")
public record QuestionIdResponse(@Schema(description = "题目 ID", example = "1") long id) {
}
