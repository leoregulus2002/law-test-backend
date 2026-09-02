package cn.yanzongkeji.lawtest.question.domain.model;

import java.util.Objects;

/** 题库聚合根，仅管理题库元数据。 */
public final class QuestionBank {
    private final QuestionBankId id;
    private final QuestionBankCode code;
    private final String name;
    private final String sourceFileName;

    private QuestionBank(QuestionBankId id, QuestionBankCode code, String name, String sourceFileName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sourceFileName = sourceFileName;
    }

    public static QuestionBank create(String sourceFileName, QuestionBankCode code) {
        String normalizedFileName = normalizeFileName(sourceFileName);
        String name = normalizedFileName.replaceFirst("(?i)\\.docx$", "").strip();
        if (name.isBlank())
            throw new IllegalArgumentException("题库名称不能为空");
        return new QuestionBank(null, Objects.requireNonNull(code, "题库编码不能为空"), name, normalizedFileName);
    }

    public static QuestionBank reconstitute(QuestionBankId id, QuestionBankCode code, String name,
            String sourceFileName) {
        return new QuestionBank(Objects.requireNonNull(id, "题库 ID 不能为空"), Objects.requireNonNull(code, "题库编码不能为空"),
                requireText(name, "题库名称不能为空"), normalizeFileName(sourceFileName));
    }

    public QuestionBankId id() {
        return id;
    }

    public QuestionBankCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public String sourceFileName() {
        return sourceFileName;
    }

    private static String normalizeFileName(String value) {
        String fileName = requireText(value, "原始文件名不能为空");
        int separator = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        return requireText(fileName.substring(separator + 1), "原始文件名不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException(message);
        return value.strip();
    }
}
