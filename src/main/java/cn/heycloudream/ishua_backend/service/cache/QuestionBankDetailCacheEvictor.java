package cn.heycloudream.ishua_backend.service.cache;

import cn.heycloudream.ishua_backend.common.constants.IShuaRedisCacheConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 热点题库详情缓存失效（题库或试题发生写操作时调用）。
 *
 * @author C1ouD
 */
@SuppressWarnings("null")
@Component
@RequiredArgsConstructor
public class QuestionBankDetailCacheEvictor {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 删除指定题库的聚合缓存，使下次访问回源 MySQL。
     */
    public void evict(long bankId) {
        stringRedisTemplate.delete(IShuaRedisCacheConstants.bankDetailKey(bankId));
    }
}
