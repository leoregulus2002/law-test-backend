package cn.yanzongkeji.lawtest.question.application.dto;

import java.util.List;

/** 题目 ID 的游标分页结果。 */
public record QuestionIdCursorPage(List<Long> items, Long nextCursor, boolean hasNext, long total) {
}
