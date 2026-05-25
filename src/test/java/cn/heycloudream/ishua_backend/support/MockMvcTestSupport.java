package cn.heycloudream.ishua_backend.support;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * {@code MockMvc} 请求构造辅助：统一 Bearer Token 头格式。
 */
public final class MockMvcTestSupport {

    private MockMvcTestSupport() {
    }

    public static MockHttpServletRequestBuilder withBearerAuth(
            MockHttpServletRequestBuilder builder, Long userId) {
        return builder.header(HttpHeaders.AUTHORIZATION, JwtTestHelper.bearerAuthorization(userId));
    }

    public static MockHttpServletRequestBuilder withBearerAuth(MockHttpServletRequestBuilder builder) {
        return withBearerAuth(builder, JwtTestHelper.DEFAULT_TEST_USER_ID);
    }
}
