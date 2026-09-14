package cn.yanzongkeji.lawtest.question.interfaces.rest.request;

/** 当前用户提交的主观题作答内容。 */
public record SubjectiveAnswerSubmissionRequest(String answer) {
  public String normalizedAnswer() {
    if (answer == null || answer.isBlank()) {
      throw new IllegalArgumentException("主观题答案不能为空");
    }
    String normalized = answer.strip();
    if (normalized.length() > 10_000) {
      throw new IllegalArgumentException("主观题答案不能超过 10000 个字符");
    }
    return normalized;
  }
}
