package cn.yanzongkeji.lawtest.exam.interfaces.rest.request;

import java.math.BigDecimal;
import java.util.List;

/** 用户在每次开考前提交的即时组卷参数。 */
public record MockExamStartRequest(
    String title,
    int durationMinutes,
    BigDecimal passingScore,
    List<Long> questionBankIds,
    List<MockExamRuleRequest> rules) {}
