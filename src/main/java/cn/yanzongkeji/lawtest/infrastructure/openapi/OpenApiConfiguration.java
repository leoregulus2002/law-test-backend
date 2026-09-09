package cn.yanzongkeji.lawtest.infrastructure.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.http.HttpHeaders;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Application-level OpenAPI metadata and Knife4j document groups. */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    OpenAPI lawTestOpenApi() {
        return new OpenAPI().components(new Components().addSecuritySchemes(HttpHeaders.AUTHORIZATION,
                new SecurityScheme().name(HttpHeaders.AUTHORIZATION).type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")))
                .info(new Info().title("法考系统接口").version("v1")
                .description("法考题库、题目管理与用户认证接口。"));
    }

    @Bean
    GroupedOpenApi userOpenApi() {
        return GroupedOpenApi.builder().group("用户中心")
                .pathsToMatch("/api/v1/auth/**", "/api/v1/users/**")
                .addOpenApiCustomizer(OpenApiConfiguration::addAuthorizationHeader)
                .build();
    }

    @Bean
    GroupedOpenApi questionBankOpenApi() {
        return GroupedOpenApi.builder().group("题库管理")
                .pathsToMatch("/api/v1/question-banks/**", "/api/v1/questions/**")
                .addOpenApiCustomizer(OpenApiConfiguration::addAuthorizationHeader)
                .build();
    }

    private static void addAuthorizationHeader(OpenAPI openApi) {
        openApi.getPaths().forEach((path, pathItem) -> {
            if (!path.startsWith("/api/v1/auth/"))
                pathItem.readOperations().forEach(operation -> operation.addParametersItem(new Parameter()
                        .in("header").name(HttpHeaders.AUTHORIZATION).required(true)
                        .description("Bearer <accessToken>").schema(new StringSchema())));
        });
    }
}
