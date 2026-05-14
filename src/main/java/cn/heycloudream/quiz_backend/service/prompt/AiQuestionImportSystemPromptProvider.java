package cn.heycloudream.quiz_backend.service.prompt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 从 classpath 加载智能导入系统提示词，避免在 Java 源码中维护超长字符串。
 *
 * @author C1ouD
 */
@SuppressWarnings("null")
@Component
public class AiQuestionImportSystemPromptProvider {

    private final String systemPrompt;

    public AiQuestionImportSystemPromptProvider(@Value("classpath:prompts/ai-import-system.txt") Resource promptResource)
            throws IOException {
        this.systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }
}
