package cn.yanzongkeji.lawtest.question.domain.model;

import java.util.Locale;
import java.util.Objects;

public record QuestionBankCode(String value) {
    public QuestionBankCode {
        Objects.requireNonNull(value, "题库编码不能为空");
        value = value.strip().toLowerCase(Locale.ROOT);
        if (!value.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("题库编码必须是 64 位 SHA-256 十六进制摘要");
    }
}
