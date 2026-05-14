package cn.heycloudream.quiz_backend.exception;

import cn.heycloudream.quiz_backend.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理，将业务与参数校验错误统一为 {@link Result}。
 *
 * @author C1ouD
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 文件上传大小超限：由 Spring {@code MaxUploadSizeExceededException} 触发。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(400, "上传文件过大，最大支持 10 MB");
    }

    /** 限流拦截：返回 429 Too Many Requests。 */
    @ExceptionHandler(RateLimitException.class)
    public Result<Void> handleRateLimit(RateLimitException e) {
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

    /** Redis / MySQL 等数据访问异常，对外统一返回 500。 */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccess(DataAccessException e) {
        log.error("数据访问异常", e);
        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务繁忙，请稍后重试");
    }

    /** 兜底：未预期的运行时异常，避免直接暴露 Tomcat 错误页。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务繁忙，请稍后重试");
    }

    private static String firstFieldErrorMessage(BindingResult br) {
        return br.getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("参数校验失败");
    }
}
