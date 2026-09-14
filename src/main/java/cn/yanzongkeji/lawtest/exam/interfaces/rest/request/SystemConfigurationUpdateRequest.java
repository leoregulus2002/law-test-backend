package cn.yanzongkeji.lawtest.exam.interfaces.rest.request;

/** OpenAPI 配置中的 apiKey 留空时保留数据库里的原密钥。 */
public record SystemConfigurationUpdateRequest(
    Boolean enabled, String baseUrl, String apiKey, String model, Integer timeoutSeconds) {}
