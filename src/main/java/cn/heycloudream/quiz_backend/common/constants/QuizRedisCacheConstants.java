package cn.heycloudream.quiz_backend.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 刷题热点场景 Redis Key 与占位符约定。
 *
 * @author atlas
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuizRedisCacheConstants {

    /** 题库不存在时的空值占位，防缓存穿透（短 TTL）。 */
    public static final String NULL_BANK_PLACEHOLDER = "NULL_BANK";

    /** 热点题库详情（题库 VO + 全量试题列表）缓存前缀。 */
    private static final String BANK_DETAIL_PREFIX = "smart_quiz:bank_detail:";

    /** 智能导入异步任务状态：{@code smart_quiz:import_status:{bankId}}。 */
    private static final String AI_IMPORT_STATUS_PREFIX = "smart_quiz:import_status:";

    /** 导入状态 Key TTL（秒）：1 小时，避免脏数据长期占用。 */
    public static final int AI_IMPORT_STATUS_TTL_SECONDS = 3600;

    /** 重建缓存时的互斥锁前缀。 */
    private static final String BANK_DETAIL_LOCK_PREFIX = "smart_quiz:bank_detail:lock:";

    /** 重建锁持有时间（秒），需大于单次 MySQL 查询耗时。 */
    public static final int BANK_DETAIL_LOCK_TTL_SECONDS = 25;

    /** 空值缓存 TTL（秒）：5 分钟。 */
    public static final int NULL_BANK_CACHE_TTL_SECONDS = 300;

    /** 正常缓存 TTL 下限（秒）：30 分钟。 */
    public static final int BANK_DETAIL_CACHE_TTL_MIN_SECONDS = 30 * 60;

    /** 正常缓存 TTL 上限（秒）：40 分钟（含上界）。 */
    public static final int BANK_DETAIL_CACHE_TTL_MAX_SECONDS = 40 * 60;

    /**
     * 热点题库详情主 Key：{@code smart_quiz:bank_detail:{bankId}}。
     */
    public static String bankDetailKey(long bankId) {
        return BANK_DETAIL_PREFIX + bankId;
    }

    /**
     * 热点题库详情重建互斥锁 Key。
     */
    public static String bankDetailLockKey(long bankId) {
        return BANK_DETAIL_LOCK_PREFIX + bankId;
    }

    /**
     * 智能导入状态 Key，供前端轮询（经服务端接口读取，不暴露 Redis 直连）。
     */
    public static String importStatusKey(long bankId) {
        return AI_IMPORT_STATUS_PREFIX + bankId;
    }
}
