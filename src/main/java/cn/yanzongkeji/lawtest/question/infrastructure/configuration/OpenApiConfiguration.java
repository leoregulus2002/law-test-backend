package cn.yanzongkeji.lawtest.question.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI lawTestOpenApi() {
        return new OpenAPI().info(new Info().title("法考题目解析接口").version("v1")
                .description("上传包含题号、题目、选项、答案、解析五列字段的 .docx 表格，返回解析后的题目 JSON。"));
    }
}
