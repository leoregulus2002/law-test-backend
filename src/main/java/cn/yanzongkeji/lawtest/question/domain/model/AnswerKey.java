package cn.yanzongkeji.lawtest.question.domain.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 正确答案值对象，保留 Word 中的答案顺序。
 */
public record AnswerKey(List<String> optionLabels) {

    public AnswerKey {
        if (optionLabels == null || optionLabels.isEmpty()) {
            throw new IllegalArgumentException("答案不能为空");
        }

        List<String> normalizedLabels = optionLabels.stream().filter(Objects::nonNull).map(String::strip)
                .map(label -> label.toUpperCase(Locale.ROOT)).toList();
        if (normalizedLabels.size() != optionLabels.size()
                || normalizedLabels.stream().anyMatch(label -> !label.matches("[A-Z]"))) {
            throw new IllegalArgumentException("答案必须由单个大写选项标识组成");
        }
        if (new LinkedHashSet<>(normalizedLabels).size() != normalizedLabels.size()) {
            throw new IllegalArgumentException("答案不能包含重复选项");
        }
        optionLabels = List.copyOf(normalizedLabels);
    }
}
