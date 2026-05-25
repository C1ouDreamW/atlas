package cn.heycloudream.ishua_backend.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装。
 *
 * @param <T> 业务数据类型
 * @author C1ouD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = """
        统一 API 响应包装。HTTP 状态码通常为 200；
        业务成败以 code 为准（200 成功，400/401/403/404/409/429/500 等为业务失败）。
        """)
public class Result<T> {

    /** 业务成功状态码。 */
    public static final int SUCCESS_CODE = 200;

    @Schema(description = "业务状态码：200 成功；4xx/5xx 为业务失败（HTTP 仍多为 200）", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "业务数据；失败时常为 null；Void 接口成功时也为 null")
    private T data;

    /**
     * 成功响应（带数据）。
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder().code(SUCCESS_CODE).message("success").data(data).build();
    }

    /**
     * 失败响应。
     */
    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder().code(code).message(message).data(null).build();
    }
}
