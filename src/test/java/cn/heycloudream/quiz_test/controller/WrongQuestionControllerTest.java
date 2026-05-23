package cn.heycloudream.quiz_test.controller;

import cn.heycloudream.quiz_backend.controller.WrongQuestionController;
import cn.heycloudream.quiz_test.QuizTestApplication;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.mapper.SysUserMapper;
import cn.heycloudream.quiz_backend.service.WrongQuestionService;
import cn.heycloudream.quiz_backend.support.AtlasWebMvcTestConfig;
import cn.heycloudream.quiz_backend.support.MockMvcTestSupport;
import cn.heycloudream.quiz_backend.support.WebMvcAuthTestSupport;
import cn.heycloudream.quiz_backend.vo.wrong.WrongQuestionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link WrongQuestionController} Web 切片测试。
 */
@WebMvcTest(controllers = WrongQuestionController.class)
@ContextConfiguration(classes = {QuizTestApplication.class, WrongQuestionController.class})
@Import(AtlasWebMvcTestConfig.class)
@ActiveProfiles("test")
class WrongQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WrongQuestionService wrongQuestionService;

    @MockBean
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("GET /: 无 Token → code=401")
    void pageWrongQuestions_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/wrong-questions")
                        .param("current", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /: 分页参数缺失 → code=400")
    void pageWrongQuestions_missingPageParams_shouldReturn400() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        get("/api/v1/wrong-questions"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /: 成功 → code=200")
    void pageWrongQuestions_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);
        when(wrongQuestionService.pageWrongQuestions(any(), any())).thenReturn(
                PageResultVO.<WrongQuestionVO>builder()
                        .total(1L)
                        .records(List.of(WrongQuestionVO.builder().id(1L).questionId(10L).build()))
                        .build());

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        get("/api/v1/wrong-questions")
                                .param("current", "1")
                                .param("pageSize", "10"),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("DELETE /{id}: 成功 → code=200")
    void removeWrongQuestion_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/v1/wrong-questions/5"),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
