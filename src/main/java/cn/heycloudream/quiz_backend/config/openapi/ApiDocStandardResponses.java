package cn.heycloudream.quiz_backend.config.openapi;

import cn.heycloudream.quiz_backend.common.vo.Result;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口遵循统一 {@link Result} 包装及「HTTP 200 + body.code」错误约定（仅文档用途）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = """
                        HTTP 200。成功时 body.code=200 且 data 有值（Void 接口 data 为 null）。
                        业务失败时 HTTP 仍为 200，body.code 见接口说明（常见 400/401/403/404/409/429/500），data 多为 null。
                        """,
                content = @Content(schema = @Schema(implementation = Result.class))
        )
})
public @interface ApiDocStandardResponses {
}
