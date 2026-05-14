package cn.heycloudream.quiz_backend.service;

import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankDetailBundleVO;

/**
 * 期末周热点公开题库详情（含全量试题）查询，整合 Redis 与 MySQL。
 *
 * @author C1ouD
 */
public interface QuestionBankHotDetailService {

    /**
     * 获取公开热点题库的聚合数据：先读 Redis，未命中则在 SETNX 互斥保护下回源 MySQL 并回写缓存。
     *
     * @param bankId 题库主键
     * @return 题库 VO + 全量试题列表
     * @throws cn.heycloudream.quiz_backend.exception.BusinessException 题库不存在或题库非公开热点
     */
    QuestionBankDetailBundleVO getHotPublicBankDetail(long bankId);
}
