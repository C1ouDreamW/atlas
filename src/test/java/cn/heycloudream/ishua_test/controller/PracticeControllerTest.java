package cn.heycloudream.ishua_test.controller;

import cn.heycloudream.ishua_backend.controller.PracticeController;
import cn.heycloudream.ishua_test.IShuaTestApplication;
import cn.heycloudream.ishua_backend.dto.practice.AnswerSubmitDTO;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.SysUserMapper;
import cn.heycloudream.ishua_backend.service.PracticeService;
import cn.heycloudream.ishua_backend.support.AtlasWebMvcTestConfig;
import cn.heycloudream.ishua_backend.support.MockMvcTestSupport;
import cn.heycloudream.ishua_backend.support.WebMvcAuthTestSupport;
import cn.heycloudream.ishua_backend.vo.practice.AnswerSubmitResultVO;
import cn.heycloudream.ishua_backend.vo.practice.PracticeQuestionVO;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link PracticeController} Web 切片测试。
 */
@WebMvcTest(controllers = PracticeController.class)
@ContextConfiguration(classes = {IShuaTestApplication.class, PracticeController.class})
@Import(AtlasWebMvcTestConfig.class)
@ActiveProfiles("test")
class PracticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PracticeService practiceService;

    @MockBean
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("GET /banks/{id}/questions: 无 Token → code=401")
    void listPracticeQuestions_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/practice/banks/1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /banks/{id}/questions: 题库不存在 → code=404")
    void listPracticeQuestions_bankNotFound_shouldReturn404() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);
        when(practiceService.listPracticeQuestions(eq(1L), eq(999L), anyBoolean()))
                .thenThrow(new BusinessException(404, "题库不存在"));

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        get("/api/v1/practice/banks/999/questions"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("GET /banks/{id}/questions: 成功 → code=200")
    void listPracticeQuestions_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);
        when(practiceService.listPracticeQuestions(1L, 1L, false))
                .thenReturn(List.of(PracticeQuestionVO.builder().id(10L).stem("题干").build()));

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        get("/api/v1/practice/banks/1/questions"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    @DisplayName("POST submit: 成功 → code=200")
    void submitAnswer_success_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);
        when(practiceService.submitAnswer(eq(1L), eq(1L), eq(10L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(AnswerSubmitResultVO.builder().correct(true).needsManualGrading(false).build());

        AnswerSubmitDTO dto = AnswerSubmitDTO.builder().userAnswer(List.of("A")).build();

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        post("/api/v1/practice/banks/1/questions/10/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.correct").value(true));
    }
}
