package cn.heycloudream.quiz_backend.exception;

/**
 * 大模型 HTTP 调用或响应解析失败时抛出，语义区别于通用 {@link IllegalStateException}。
 *
 * @author atlas
 */
public class LlmInvokeException extends RuntimeException {

    public LlmInvokeException(String message) {
        super(message);
    }

    public LlmInvokeException(String message, Throwable cause) {
        super(message, cause);
    }
}
