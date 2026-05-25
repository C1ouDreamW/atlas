package cn.heycloudream.ishua_backend.support;

import cn.heycloudream.ishua_backend.entity.QuestionBank;
import cn.heycloudream.ishua_backend.mapper.QuestionBankMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 轨道 A 验收：H2 schema.sql + data.sql 下 MyBatis-Plus 可正常访问种子数据。
 */
class H2SchemaSmokeTest extends AbstractMockRedisSpringBootTest {

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Test
    @DisplayName("H2 种子数据：公开题库 bankId=1 可查询")
    void shouldLoadSeedQuestionBank() {
        QuestionBank bank = questionBankMapper.selectById(1L);
        assertThat(bank).isNotNull();
        assertThat(bank.getUserId()).isEqualTo(1L);
        assertThat(bank.getIsPublic()).isEqualTo(1);
    }
}
