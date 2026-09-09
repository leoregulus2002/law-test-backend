package cn.yanzongkeji.lawtest.question.application.dto;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;

/** 管理端题目列表所需的轻量数据，避免为每条记录加载选项与答案。 */
public record QuestionListItem(long id, long questionBankId, String questionBankName, int number, String stem,
        QuestionType questionType, QuestionStatus status) {
}
