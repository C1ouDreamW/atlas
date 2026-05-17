package cn.heycloudream.quiz_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 全局配置（仅影响 /v3/api-docs 与 Swagger UI，不参与业务逻辑）。
 */
@Configuration
public class OpenApiConfig {

    /** Bearer JWT，与 {@link JwtAuthInterceptor} 使用的 Header 一致。 */
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI atlasOpenApi() {
        String description = """
                Atlas 智能题库后端 REST API（面向前端与联调）。

                **鉴权**
                - 除接口说明中标注「无需登录」外，请求头须携带：`Authorization: Bearer <token>`
                - token 由 `POST /api/v1/users/login` 返回
                - 白名单（无需 token）：注册、登录、`GET /question-banks/public`、`GET .../hot-practice-detail`

                **统一响应 `Result<T>`**
                - 字段：`code`（业务码）、`message`、`data`
                - 成功：`code = 200`，`message = "success"`
                - 业务失败：**HTTP 状态码仍为 200**，以 body 中 `code` 区分：
                  `400` 参数错误、`401` 未登录/Token 无效、`403` 无权限、`404` 资源不存在、
                  `409` 冲突（如导入进行中）、`429` 限流、`500` 服务错误；此时 `data` 通常为 null

                **分页 `PageResultVO<T>`**（作为 `Result.data`）
                - `total`：总记录数；`records`：当前页列表（非 list/items）
                - Query 参数：`current`（从 1 开始）、`pageSize`（最大 100）

                **试题 JSON 字段**
                - 详情/列表接口：`optionsJson`、`answerJson` 为 **JSON 数组的字符串**（如 `["A","B"]`）
                - AI 预览/批量确认：`QuestionPreviewVO` 使用 `options`、`answer` **数组**，入库后读接口为 JSON 字符串
                """;

        return new OpenAPI()
                .info(new Info()
                        .title("Atlas 智能题库 API")
                        .version("v1")
                        .description(description))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("登录接口返回的 JWT，Header：Authorization: Bearer {token}")));
    }
}
