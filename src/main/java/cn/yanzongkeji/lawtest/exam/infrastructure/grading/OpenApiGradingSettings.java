package cn.yanzongkeji.lawtest.exam.infrastructure.grading;

/** 键为 openapi 的系统配置值。 */
public record OpenApiGradingSettings(
    boolean enabled, String baseUrl, String apiKey, String model, int timeoutSeconds) {

  public OpenApiGradingSettings {
    baseUrl = normalize(baseUrl);
    apiKey = normalize(apiKey);
    model = normalize(model);
    if (timeoutSeconds < 5 || timeoutSeconds > 120) timeoutSeconds = 45;
  }

  public boolean canGrade() {
    return enabled && !baseUrl.isBlank() && !apiKey.isBlank() && !model.isBlank();
  }

  private static String normalize(String value) {
    return value == null ? "" : value.strip();
  }
}
