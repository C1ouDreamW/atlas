package cn.heycloudream.ishua_backend.config.openapi;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口遵循统一 Result 包装及「HTTP 200 + body.code」错误约定（仅文档用途）。
 * <p>具体 {@code data} 结构由 {@link ResultResponseOperationCustomizer} 按方法返回泛型展开。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiResponses({
        @ApiResponse(responseCode = "200", description = ApiDocStandardResponses.HTTP_200_DESCRIPTION)
})
public @interface ApiDocStandardResponses {

    String HTTP_200_DESCRIPTION = """
            HTTP 200。成功时 body.code=200 且 data 有值（Void 接口 data 为 null）。
            业务失败时 HTTP 仍为 200，body.code 见接口说明（常见 400/401/403/404/409/429/500），data 多为 null。
            响应 body 的 data 字段结构见本接口 Schema 示例（已按泛型展开）。
            """;
}
