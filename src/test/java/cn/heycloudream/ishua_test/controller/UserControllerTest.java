package cn.heycloudream.ishua_test.controller;

import cn.heycloudream.ishua_backend.controller.UserController;
import cn.heycloudream.ishua_test.IShuaTestApplication;
import cn.heycloudream.ishua_backend.dto.user.UserLoginDTO;
import cn.heycloudream.ishua_backend.dto.user.UserRegisterDTO;
import cn.heycloudream.ishua_backend.exception.BusinessException;
import cn.heycloudream.ishua_backend.mapper.SysUserMapper;
import cn.heycloudream.ishua_backend.service.UserService;
import cn.heycloudream.ishua_backend.support.AtlasWebMvcTestConfig;
import cn.heycloudream.ishua_backend.support.MockMvcTestSupport;
import cn.heycloudream.ishua_backend.support.WebMvcAuthTestSupport;
import cn.heycloudream.ishua_backend.vo.user.UserLoginVO;
import cn.heycloudream.ishua_backend.vo.user.UserMeVO;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link UserController} Web 切片测试。
 */
@WebMvcTest(controllers = UserController.class)
@ContextConfiguration(classes = {IShuaTestApplication.class, UserController.class})
@Import(AtlasWebMvcTestConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private SysUserMapper sysUserMapper;

    @Test
    @DisplayName("POST /register: 参数校验失败 → code=400")
    void register_validationFail_shouldReturn400() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("ab");
        dto.setPassword("123");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /register: 成功 → code=200")
    void register_success_shouldReturn200() throws Exception {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("newuser");
        dto.setPassword("123456");

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).register(any(UserRegisterDTO.class));
    }

    @Test
    @DisplayName("POST /login: 账号或密码错误 → code=401")
    void login_invalidCredentials_shouldReturn401() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("nobody");
        dto.setPassword("wrong");

        doThrow(new BusinessException(401, "账号或密码错误")).when(userService).login(any());

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("POST /login: 成功 → code=200 且返回 token")
    void login_success_shouldReturnToken() throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("123456");

        when(userService.login(any())).thenReturn(UserLoginVO.builder()
                .token("jwt")
                .userId(1L)
                .username("alice")
                .role("USER")
                .build());

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt"));
    }

    @Test
    @DisplayName("GET /me: 无 Token → code=401")
    void me_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /me: 有效 Token → code=200")
    void me_withToken_shouldReturn200() throws Exception {
        WebMvcAuthTestSupport.stubBasicUser(sysUserMapper, 1L);
        when(userService.getCurrentUser(1L)).thenReturn(UserMeVO.builder()
                .userId(1L)
                .username("testuser")
                .role("USER")
                .build());

        mockMvc.perform(MockMvcTestSupport.withBearerAuth(get("/api/v1/users/me"), 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1));
    }
}
