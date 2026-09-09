package cn.yanzongkeji.lawtest.question.application.dto;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;

/** 题目管理列表支持的组合筛选条件。 */
public record QuestionListFilter(String keyword, QuestionType questionType, QuestionStatus status, Long questionBankId) {
}
