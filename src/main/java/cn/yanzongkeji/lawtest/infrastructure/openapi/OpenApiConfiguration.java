package cn.yanzongkeji.lawtest.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-level OpenAPI metadata and Knife4j document groups. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI lawTestOpenApi() {
        return new OpenAPI().info(new Info().title("法考系统接口").version("v1")
                .description("法考题库、题目管理与用户认证接口。"));
    }

    @Bean
    GroupedOpenApi userOpenApi() {
        return GroupedOpenApi.builder().group("用户中心")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**")
                .build();
    }

    @Bean
    GroupedOpenApi questionBankOpenApi() {
        return GroupedOpenApi.builder().group("题库管理")
                .pathsToMatch("/api/v1/question-banks/**", "/api/v1/questions/**")
                .build();
    }
}
