package cn.heycloudream.quiz_test.controller;

import cn.heycloudream.quiz_backend.controller.QuestionBankController;
import cn.heycloudream.quiz_test.QuizTestApplication;
import cn.heycloudream.quiz_backend.common.vo.PageResultVO;
import cn.heycloudream.quiz_backend.dto.questionbank.QuestionBankCreateDTO;
import cn.heycloudream.quiz_backend.exception.BusinessException;
import cn.heycloudream.quiz_backend.mapper.SysUserMapper;
import cn.heycloudream.quiz_backend.service.QuestionBankHotDetailService;
import cn.heycloudream.quiz_backend.service.QuestionBankService;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.service.ai.AiImportResultStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskMetaStore;
import cn.heycloudream.quiz_backend.service.ai.AiImportTaskStatusStore;
import cn.heycloudream.quiz_backend.service.ai.ImportIdempotentService;
import cn.heycloudream.quiz_backend.support.AtlasWebMvcTestConfig;
import cn.heycloudream.quiz_backend.support.MockMvcTestSupport;
import cn.heycloudream.quiz_backend.support.WebMvcAuthTestSupport;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankDetailBundleVO;
import cn.heycloudream.quiz_backend.vo.questionbank.QuestionBankVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link QuestionBankController} Web 切片测试。
 */
@WebMvcTest(controllers = QuestionBankController.class)
@ContextConfiguration(classes = {QuizTestApplication.class, QuestionBankController.class})
@Import(AtlasWebMvcTestConfig.class)
@ActiveProfiles("test")
class QuestionBankControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionBankService questionBankService;

    @MockBean
    private QuestionService questionService;

    @MockBean
    private QuestionBankHotDetailService questionBankHotDetailService;

    @MockBean
    private ImportIdempotentService importIdempotentService;

    @MockBean
    private AiImportTaskStatusStore taskStatusStore;

    @MockBean
    private AiImportResultStore resultStore;

    @MockBean
    private AiImportTaskMetaStore taskMetaStore;

    @MockBean
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("GET /public: 白名单无需 Token → code=200")
    void pagePublicBanks_withoutToken_shouldReturn200() throws Exception {
        when(questionBankService.pagePublicBanks(any())).thenReturn(PageResultVO.<QuestionBankVO>builder()
                .total(1L)
                .records(List.of(QuestionBankVO.builder().id(1L).title("公开").build()))
                .build());

        mockMvc.perform(get("/api/v1/question-banks/public")
                        .param("current", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("GET /{id}/hot-practice-detail: 白名单无需 Token → code=200")
    void getHotPracticeDetail_withoutToken_shouldReturn200() throws Exception {
        when(questionBankHotDetailService.getHotPublicBankDetail(1L))
                .thenReturn(QuestionBankDetailBundleVO.builder()
                        .bank(QuestionBankVO.builder().id(1L).build())
                        .questions(List.of())
                        .build());

        mockMvc.perform(get("/api/v1/question-banks/1/hot-practice-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bank.id").value(1));
    }

    @Test
    @DisplayName("GET /{id}/hot-practice-detail: 非公开 → code=403")
    void getHotPracticeDetail_privateBank_shouldReturn403() throws Exception {
        when(questionBankHotDetailService.getHotPublicBankDetail(2L))
                .thenThrow(new BusinessException(403, "仅公开热点题库支持聚合刷题缓存接口"));

        mockMvc.perform(get("/api/v1/question-banks/2/hot-practice-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("GET /: 无 Token → code=401")
    void pageMyBanks_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/question-banks")
                        .param("current", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /: USER 角色 → code=403")
    void createBank_userRole_shouldReturn403() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);

        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setTitle("我的题库");
        dto.setIsPublic(0);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        post("/api/v1/question-banks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("POST /: PREMIUM 角色 → code=200")
    void createBank_premiumRole_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubPremiumUser(sysUserMapper, 1L);
        when(questionBankService.createBank(eq(1L), any())).thenReturn(99L);

        QuestionBankCreateDTO dto = new QuestionBankCreateDTO();
        dto.setTitle("高级题库");
        dto.setIsPublic(1);

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(
                        post("/api/v1/question-banks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)),
                        1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(99));
    }
}
