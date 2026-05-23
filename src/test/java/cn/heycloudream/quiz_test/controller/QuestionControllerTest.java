package cn.heycloudream.quiz_test.controller;

import cn.heycloudream.quiz_backend.controller.QuestionController;
import cn.heycloudream.quiz_test.QuizTestApplication;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.SysUserMapper;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.support.AtlasWebMvcTestConfig;
import cn.heycloudream.quiz_backend.support.MockMvcTestSupport;
import cn.heycloudream.quiz_backend.support.WebMvcAuthTestSupport;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link QuestionController} Web 切片测试。
 */
@WebMvcTest(controllers = QuestionController.class)
@ContextConfiguration(classes = {QuizTestApplication.class, QuestionController.class})
@Import(AtlasWebMvcTestConfig.class)
@ActiveProfiles("test")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionService questionService;

    @MockBean
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("GET /{id}: 无 Token → code=401")
    void getById_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/questions/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /{id}: USER 角色 → code=403")
    void getById_userRole_shouldReturn403() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(get("/api/v1/questions/100"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("GET /{id}: 试题不存在 → code=404")
    void getById_notFound_shouldReturn404() throws Exception {
        WebMvcAuthTestSupport.stubPremiumUser(sysUserMapper, 1L);
        when(questionService.getQuestionById(eq(1L), eq(100L)))
                .thenThrow(new BusinessException(404, "试题不存在或无权访问"));

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(get("/api/v1/questions/100"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("试题不存在或无权访问"));
    }

    @Test
    @DisplayName("GET /{id}: PREMIUM 成功 → code=200")
    void getById_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubPremiumUser(sysUserMapper, 1L);
        when(questionService.getQuestionById(1L, 100L)).thenReturn(
                QuestionVO.builder().id(100L).stem("题干").questionType("SINGLE").build());

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(get("/api/v1/questions/100"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("PUT /{id}: 参数校验失败 → code=400")
    void update_validationFail_shouldReturn400() throws Exception {
        WebMvcAuthTestSupport.stubPremiumUser(sysUserMapper, 1L);

        QuestionUpdateDTO dto = new QuestionUpdateDTO();
        dto.setQuestionType("");
        dto.setStem("");

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        put("/api/v1/questions/100")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("DELETE /{id}: PREMIUM 成功 → code=200")
    void delete_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubPremiumUser(sysUserMapper, 1L);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(delete("/api/v1/questions/100"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
