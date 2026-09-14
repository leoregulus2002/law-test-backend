package cn.yanzongkeji.lawtest.exam.infrastructure.configuration;

import cn.yanzongkeji.lawtest.exam.infrastructure.grading.OpenApiGradingSettings;
import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject.SystemConfigurationDO;
import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.mapper.SystemConfigurationMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** 管理系统配置字典，当前提供 openapi 模型评分配置。 */
@Service
@RequiredArgsConstructor
public class SystemConfigurationService {
  public static final String OPENAPI_CONFIGURATION_KEY = "openapi";
  private final SystemConfigurationMapper configurations;
  private final ObjectMapper objectMapper;

  public OpenApiGradingSettings openApiGradingSettings() {
    SystemConfigurationDO configuration = configurations.selectById(OPENAPI_CONFIGURATION_KEY);
    if (configuration == null) return defaultOpenApiSettings();
    try {
      return objectMapper.readValue(configuration.getValue(), OpenApiGradingSettings.class);
    } catch (Exception exception) {
      throw new IllegalStateException("OpenAPI 配置格式错误", exception);
    }
  }

  @Transactional
  public OpenApiGradingSettings saveOpenApiGradingSettings(
      Boolean enabled, String baseUrl, String apiKey, String model, Integer timeoutSeconds) {
    if (enabled == null) throw new IllegalArgumentException("请设置是否启用模型评分");
    OpenApiGradingSettings existing = openApiGradingSettings();
    int timeout = timeoutSeconds == null ? existing.timeoutSeconds() : timeoutSeconds;
    if (timeout < 5 || timeout > 120) {
      throw new IllegalArgumentException("模型评分超时时间需在 5 到 120 秒之间");
    }
    String retainedApiKey = apiKey == null || apiKey.isBlank() ? existing.apiKey() : apiKey.strip();
    OpenApiGradingSettings updated =
        new OpenApiGradingSettings(enabled, baseUrl, retainedApiKey, model, timeout);
    if (updated.enabled() && !updated.canGrade()) {
      throw new IllegalArgumentException("启用模型评分前，请完整填写接口地址、密钥和模型名称");
    }
    SystemConfigurationDO configuration = new SystemConfigurationDO();
    configuration.setKey(OPENAPI_CONFIGURATION_KEY);
    configuration.setUpdatedAt(Instant.now());
    try {
      configuration.setValue(objectMapper.writeValueAsString(updated));
    } catch (Exception exception) {
      throw new IllegalStateException("保存 OpenAPI 配置失败", exception);
    }
    if (configurations.updateById(configuration) == 0) configurations.insert(configuration);
    return updated;
  }

  private static OpenApiGradingSettings defaultOpenApiSettings() {
    return new OpenApiGradingSettings(false, "", "", "", 45);
  }
}
