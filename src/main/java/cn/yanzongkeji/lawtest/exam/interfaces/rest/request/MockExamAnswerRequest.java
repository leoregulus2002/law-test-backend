package cn.yanzongkeji.lawtest.exam.interfaces.rest.request;

import java.util.List;

public record MockExamAnswerRequest(List<String> selectedAnswers, String subjectiveAnswer) {}
