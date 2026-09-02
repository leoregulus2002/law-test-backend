package cn.yanzongkeji.lawtest.question.application.dto;

import java.util.List;

public record QuestionCommand(int number, String stem, List<Option> options, List<String> answers, String analysis) {
    public record Option(String label, String content) {
    }
}
