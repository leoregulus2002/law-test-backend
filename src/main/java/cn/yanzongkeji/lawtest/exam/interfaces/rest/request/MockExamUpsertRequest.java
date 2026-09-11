package cn.yanzongkeji.lawtest.exam.interfaces.rest.request;

import java.math.BigDecimal;
import java.util.List;

public record MockExamUpsertRequest(
    String title,
    int durationMinutes,
    BigDecimal passingScore,
    List<Long> questionBankIds,
    List<MockExamRuleRequest> rules) {}
