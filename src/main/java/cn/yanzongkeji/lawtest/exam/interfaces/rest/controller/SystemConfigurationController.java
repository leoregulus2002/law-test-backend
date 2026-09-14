package cn.yanzongkeji.lawtest.exam.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.exam.infrastructure.configuration.SystemConfigurationService;
import cn.yanzongkeji.lawtest.exam.interfaces.rest.request.SystemConfigurationUpdateRequest;
import cn.yanzongkeji.lawtest.exam.interfaces.rest.response.SystemConfigurationResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端系统配置字典。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/system-configurations")
public class SystemConfigurationController {
  private final SystemConfigurationService configurations;

  @GetMapping
  @Operation(summary = "查询系统配置字典")
  public ResponseEntity<List<SystemConfigurationResponse>> list() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(List.of(SystemConfigurationResponse.openApi(configurations.openApiGradingSettings())));
  }

  @PutMapping("/{key}")
  @Operation(summary = "保存系统配置项")
  public SystemConfigurationResponse update(
      @PathVariable String key, @RequestBody SystemConfigurationUpdateRequest request) {
    if (!SystemConfigurationService.OPENAPI_CONFIGURATION_KEY.equals(key)) {
      throw new IllegalArgumentException("不支持的系统配置项");
    }
    return SystemConfigurationResponse.openApi(
        configurations.saveOpenApiGradingSettings(
            request.enabled(),
            request.baseUrl(),
            request.apiKey(),
            request.model(),
            request.timeoutSeconds()));
  }
}
