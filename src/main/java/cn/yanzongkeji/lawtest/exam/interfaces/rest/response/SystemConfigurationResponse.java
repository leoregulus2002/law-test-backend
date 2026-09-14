package cn.yanzongkeji.lawtest.exam.interfaces.rest.response;

import cn.yanzongkeji.lawtest.exam.infrastructure.grading.OpenApiGradingSettings;

/** 系统配置字典项。API 密钥仅返回是否已配置，不返回密钥正文。 */
public record SystemConfigurationResponse(
    String key,
    String name,
    String description,
    boolean enabled,
    String baseUrl,
    boolean apiKeyConfigured,
    String model,
    int timeoutSeconds) {

  public static SystemConfigurationResponse openApi(OpenApiGradingSettings settings) {
    return new SystemConfigurationResponse(
        "openapi",
        "OpenAPI 配置",
        "用于模拟考试主观题的模型自动评分。",
        settings.enabled(),
        settings.baseUrl(),
        !settings.apiKey().isBlank(),
        settings.model(),
        settings.timeoutSeconds());
  }
}
