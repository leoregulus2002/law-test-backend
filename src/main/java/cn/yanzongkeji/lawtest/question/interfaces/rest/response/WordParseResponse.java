package cn.yanzongkeji.lawtest.question.interfaces.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Word 题目解析结果")
public record WordParseResponse(@Schema(description = "原始文件名", example = "2023年客观题卷一多选.docx") String fileName,
        @Schema(description = "题库 ID") long questionBankId, @Schema(description = "题库内容摘要") String questionBankCode,
        @Schema(description = "是否为本次新导入") boolean imported,
        @Schema(description = "解析出的题目数", example = "49") int questionCount,
        @Schema(description = "题目列表") List<QuestionResponse> questions) {
}
