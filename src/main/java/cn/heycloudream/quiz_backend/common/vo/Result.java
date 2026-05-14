package cn.heycloudream.quiz_backend.common.vo;

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
@Schema(description = "统一 API 响应")
public class Result<T> {

    /** 业务成功状态码。 */
    public static final int SUCCESS_CODE = 200;

    @Schema(description = "业务状态码", example = "200")
    private int code;

    @Schema(description = "提示信息", example = "success")
    private String message;

    @Schema(description = "业务数据")
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
