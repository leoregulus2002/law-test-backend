package cn.yanzongkeji.lawtest.question.application.dto;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import java.util.List;

public record QuestionCommand(int number, String stem, List<Option> options, List<String> answers, QuestionType questionType,
        String analysis) {
    public record Option(String label, String content) {
    }
}
