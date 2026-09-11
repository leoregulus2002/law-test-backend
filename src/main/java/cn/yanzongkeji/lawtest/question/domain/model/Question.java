package cn.yanzongkeji.lawtest.question.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** 题目聚合根。所有可编辑内容必须通过该聚合校验。 */
public final class Question {
  private final QuestionId id;
  private final QuestionBankId questionBankId;
  private QuestionNumber number;
  private String stem;
  private List<QuestionOption> options;
  private AnswerKey answerKey;
  private QuestionType type;
  private String analysis;
  private QuestionStatus status;

  private Question(
      QuestionId id,
      QuestionBankId questionBankId,
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis,
      QuestionStatus status) {
    this.id = id;
    this.questionBankId = questionBankId;
    this.number = number;
    this.stem = stem;
    this.options = options;
    this.answerKey = answerKey;
    this.type = type;
    this.analysis = analysis;
    this.status = status;
  }

  /** Word 解析阶段尚未得到题库 ID 的临时题目。 */
  public static Question create(
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis) {
    return build(
        null, null, number, stem, options, answerKey, type, analysis, QuestionStatus.ACTIVE);
  }

  public static Question create(
      QuestionBankId questionBankId,
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis) {
    return build(
        null,
        Objects.requireNonNull(questionBankId, "题库 ID 不能为空"),
        number,
        stem,
        options,
        answerKey,
        type,
        analysis,
        QuestionStatus.ACTIVE);
  }

  public static Question reconstitute(
      QuestionId id,
      QuestionBankId questionBankId,
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis) {
    return reconstitute(
        id,
        questionBankId,
        number,
        stem,
        options,
        answerKey,
        type,
        analysis,
        QuestionStatus.ACTIVE);
  }

  public static Question reconstitute(
      QuestionId id,
      QuestionBankId questionBankId,
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis,
      QuestionStatus status) {
    return build(
        Objects.requireNonNull(id, "题目 ID 不能为空"),
        Objects.requireNonNull(questionBankId, "题库 ID 不能为空"),
        number,
        stem,
        options,
        answerKey,
        type,
        analysis,
        status);
  }

  private static Question build(
      QuestionId id,
      QuestionBankId questionBankId,
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis,
      QuestionStatus status) {
    Objects.requireNonNull(number, "题号不能为空");
    Objects.requireNonNull(options, "选项不能为空");
    Objects.requireNonNull(answerKey, "答案不能为空");
    Objects.requireNonNull(type, "题型不能为空");
    Objects.requireNonNull(status, "题目状态不能为空");
    String normalizedStem = requireText(stem, "题干不能为空");
    if (type != QuestionType.SUBJECTIVE && answerKey.optionLabels().isEmpty())
      throw new IllegalArgumentException("答案不能为空");
    if (type != QuestionType.SUBJECTIVE && options.isEmpty())
      throw new IllegalArgumentException("题目至少需要一个选项");
    Set<String> labels = new HashSet<>();
    for (QuestionOption option : options) {
      if (option == null) throw new IllegalArgumentException("选项不能为空");
      if (!labels.add(option.label()))
        throw new IllegalArgumentException("选项标识不能重复: " + option.label());
    }
    for (String answer : answerKey.optionLabels()) {
      if (!labels.contains(answer)) throw new IllegalArgumentException("答案引用了不存在的选项: " + answer);
    }
    if (type == QuestionType.SUBJECTIVE && !answerKey.optionLabels().isEmpty())
      throw new IllegalArgumentException("主观题不能设置选项答案，请将参考答案填写在解析中");
    return new Question(
        id,
        questionBankId,
        number,
        normalizedStem,
        List.copyOf(options),
        answerKey,
        type,
        analysis == null ? "" : analysis.strip(),
        status);
  }

  public void revise(
      QuestionNumber number,
      String stem,
      List<QuestionOption> options,
      AnswerKey answerKey,
      QuestionType type,
      String analysis) {
    Question replacement =
        build(id, questionBankId, number, stem, options, answerKey, type, analysis, status);
    this.number = replacement.number;
    this.stem = replacement.stem;
    this.options = replacement.options;
    this.answerKey = replacement.answerKey;
    this.type = replacement.type;
    this.analysis = replacement.analysis;
  }

  public QuestionId id() {
    return id;
  }

  public QuestionBankId questionBankId() {
    return questionBankId;
  }

  public QuestionNumber number() {
    return number;
  }

  public String stem() {
    return stem;
  }

  public List<QuestionOption> options() {
    return options;
  }

  public AnswerKey answerKey() {
    return answerKey;
  }

  public QuestionType type() {
    return type;
  }

  public String analysis() {
    return analysis;
  }

  public QuestionStatus status() {
    return status;
  }

  public void changeStatus(QuestionStatus status) {
    this.status = Objects.requireNonNull(status, "题目状态不能为空");
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    return value.strip();
  }
}
