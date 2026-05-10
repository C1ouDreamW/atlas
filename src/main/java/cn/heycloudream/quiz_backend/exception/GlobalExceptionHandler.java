package cn.heycloudream.quiz_backend.exception;

import cn.heycloudream.quiz_backend.common.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理，将业务与参数校验错误统一为 {@link Result}。
 *
 * @author atlas
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 同步路径若误调大模型客户端，将 LLM 失败映射为 502，避免未处理异常直达容器。
     */
    @ExceptionHandler(LlmInvokeException.class)
    public Result<Void> handleLlmInvoke(LlmInvokeException e) {
        return Result.fail(HttpStatus.BAD_GATEWAY.value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        return Result.fail(400, firstFieldErrorMessage(e.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.fail(400, firstFieldErrorMessage(e.getBindingResult()));
    }

    private static String firstFieldErrorMessage(BindingResult br) {
        return br.getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("参数校验失败");
    }
}
