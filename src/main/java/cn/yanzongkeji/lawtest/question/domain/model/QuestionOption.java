package cn.yanzongkeji.lawtest.question.domain.model;

import java.util.Locale;

/**
 * 单个选项值对象。
 */
public record QuestionOption(String label, String content) {

    public QuestionOption {
        label = normalizeLabel(label);
        content = requireText(content, "选项内容不能为空");
    }

    private static String normalizeLabel(String label) {
        String normalized = requireText(label, "选项标识不能为空").toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]")) {
            throw new IllegalArgumentException("选项标识必须是单个大写英文字母");
        }
        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
