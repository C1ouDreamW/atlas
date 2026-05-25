package cn.heycloudream.ishua_backend.config;

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

                **鉴权（JWT）**
                - 除接口说明中标注「无需登录」外，请求头须携带：`Authorization: Bearer <token>`
                - token 由 `POST /api/v1/users/login` 返回；响应 `data` 含 `role` 字段，供前端菜单控制
                - 白名单（无需 token）：注册、登录、`GET /question-banks/public`、`GET .../hot-practice-detail`

                **角色与权限（RBAC）**
                - `USER`：普通用户，可刷公开题库、错题本、`GET /users/me`
                - `PREMIUM`：高级用户，在 USER 能力上可自建题库、管理试题、AI 导入（含 `ADMIN` 对写接口的归属 bypass）
                - `ADMIN`：管理员，含 PREMIUM 写能力 + `/admin/users/**` 用户管理
                - 角色等级：`ADMIN` ≥ `PREMIUM` ≥ `USER`；接口标注「最低 PREMIUM」时 ADMIN 亦可调用
                - 权限以服务端 **每次请求查库 role** 为准；管理员改角色后，下次请求即生效（无需重新登录）
                - `body.code=403`：角色不足，或无权访问资源（如 USER 调写接口、刷他人私有库）
                - `body.code=401`：未提供 Token、Token 无效、用户不存在或 DB 角色非法

                **模块最低角色速查**
                | 路径前缀 | 最低角色 |
                |----------|----------|
                | `/users/register`、`/users/login`、公开大厅、热点详情 | 无 |
                | `/practice/**`、`/wrong-questions/**`、`GET /users/me` | USER |
                | `/question-banks`（写与我的列表）、`/questions/**`、`/ai-import/**` | PREMIUM |
                | `/admin/users/**` | ADMIN |

                **统一响应 `Result<T>`**
                - 字段：`code`（业务码）、`message`、`data`
                - 成功：`code = 200`，`message = "success"`
                - 业务失败：**HTTP 状态码仍为 200**，以 body 中 `code` 区分：
                  `400` 参数错误、`401` 未登录/Token 无效、`403` 无权限、`404` 资源不存在、
                  `409` 冲突（如导入进行中）、`429` 限流、`500` 服务错误；此时 `data` 通常为 null

                **分页 `PageResultVO<T>`**（作为 `Result.data`）
                - `total`：总记录数；`records`：当前页列表（非 list/items）
                - Query 参数：`current`（从 1 开始）、`pageSize`（最大 100）

                **试题 JSON 字段（前端必读）**
                - 读接口（`QuestionVO` / 热点详情 / 管理端分页）：`optionsJson`、`answerJson` 类型为 **string**，
                  内容为 JSON 数组文本，前端需 `JSON.parse` 后使用。
                  - `optionsJson`：选项**文案**列表，如 `["TCP","UDP"]`；判断题为 `["正确","错误"]`
                  - `answerJson`：单选/多选为选项**字母** `["A"]`、`["A","C"]`；判断题为 `["T"]` 或 `["F"]`
                - 刷题拉题（`PracticeQuestionVO`）：**无** `answerJson`、`analysis`
                - AI 预览/批量确认（`QuestionPreviewVO`）：`options`、`answer` 为 **string[]**，入库后读接口变为 JSON 字符串字段
                - 题型 `questionType`：string，取值见 Schemas → `QuestionType` 枚举

                **各接口 data 模型速查（展开见接口 200 响应 Schema）**
                | 场景 | data 类型 |
                |------|-----------|
                | 公开大厅 / 我的题库分页 | `PageResult` → `records[]` = `QuestionBankVO` |
                | 热点刷题详情 | `QuestionBankDetailBundleVO`（`bank` + `questions[]`） |
                | 题库内试题分页 | `PageResult` → `records[]` = `QuestionVO` |
                | 刷题列表 | `PracticeQuestionVO[]` |
                | 提交判分 | `AnswerSubmitResultVO` |
                | AI 任务状态 | `AiImportTaskStatusVO`（PARSED 时含 `questions[]`） |
                | 错题本分页 | `PageResult` → `records[]` = `WrongQuestionVO` |

                每个接口的 **Responses → 200 → Schema** 已按 `Result<T>` 泛型自动展开，无需再到 Schemas 里猜 `data` 类型。
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
