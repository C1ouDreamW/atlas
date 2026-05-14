package cn.heycloudream.quiz_backend.enums;

/**
 * 智能导入异步任务在 Redis 中的状态枚举。
 *
 * @author C1ouD
 */
public enum AiQuestionImportRunStatus {

    /** 任务已入队，线程池正在执行。 */
    PROCESSING,

    /** 任务正常结束（含识别到 0 道有效题）。 */
    SUCCESS,

    /** 任务失败：大模型、解析或落库等环节异常。 */
    FAILED
}
