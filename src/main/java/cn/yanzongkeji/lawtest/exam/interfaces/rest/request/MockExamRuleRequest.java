package cn.yanzongkeji.lawtest.exam.interfaces.rest.request;

import java.math.BigDecimal;

public record MockExamRuleRequest(
    String questionType, String selectionMode, int questionCount, BigDecimal score) {}
