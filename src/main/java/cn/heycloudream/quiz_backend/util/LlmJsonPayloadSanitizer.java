package cn.heycloudream.quiz_backend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 对大模型输出做保守清洗，去除常见 Markdown 代码围栏后再交给 JSON 解析器。
 *
 * @author atlas
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LlmJsonPayloadSanitizer {

    /**
     * 去除首尾空白，并尽量去掉 ``` / ```json 围栏（不改变围栏内正文）。
     */
    public static String stripMarkdownCodeFence(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            } else {
                s = s.substring(3);
            }
            s = s.trim();
        }
        if (s.endsWith("```")) {
            int idx = s.lastIndexOf("```");
            if (idx >= 0) {
                s = s.substring(0, idx).trim();
            }
        }
        return s.trim();
    }
}
