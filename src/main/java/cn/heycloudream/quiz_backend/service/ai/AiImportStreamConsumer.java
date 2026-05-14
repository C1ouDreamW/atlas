package cn.heycloudream.quiz_backend.service.ai;

import cn.heycloudream.quiz_backend.client.LlmChatClient;
import cn.heycloudream.quiz_backend.common.constants.QuizRedisCacheConstants;
import cn.heycloudream.quiz_backend.dto.ai.LlmQuestionParseDTO;
import cn.heycloudream.quiz_backend.enums.AiImportTaskStatus;
import cn.heycloudream.quiz_backend.enums.QuestionType;
import cn.heycloudream.quiz_backend.service.file.FileStorageService;
import cn.heycloudream.quiz_backend.service.prompt.AiQuestionImportSystemPromptProvider;
import cn.heycloudream.quiz_backend.util.DocumentParseUtils;
import cn.heycloudream.quiz_backend.util.LlmJsonPayloadSanitizer;
import cn.heycloudream.quiz_backend.vo.ai.AiImportTaskMetaVO;
import cn.heycloudream.quiz_backend.vo.ai.QuestionPreviewVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Redis Stream 消费者：轮询 Stream，认领任务后执行「文档解析 → 大模型 → JSON 清洗 → 写预览结果」。
 * <p>
 * 与旧 {@link cn.heycloudream.quiz_backend.service.impl.AiQuestionImportAsyncProcessor} 并行运行，
 * 旧链路标记 Deprecated 后逐步切换。
 * </p>
 *
 * @author atlas
 */
@SuppressWarnings("null")
@Slf4j
@Component
@RequiredArgsConstructor
public class AiImportStreamConsumer {

    private static final Pattern LETTER = Pattern.compile("[A-Z]");

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final LlmChatClient llmChatClient;
    private final AiQuestionImportSystemPromptProvider systemPromptProvider;
    private final AiImportTaskStatusStore statusStore;
    private final AiImportResultStore resultStore;
    private final FileStorageService fileStorageService;

    /**
     * 每 3 秒轮询一次 Stream。
     */
    @Scheduled(fixedDelay = 3000)
    public void poll() {
        try {
            List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream()
                    .read(
                            Consumer.from(QuizRedisCacheConstants.TASK_STREAM_GROUP, consumerName()),
                            StreamReadOptions.empty()
                                    .count(2)
                                    .block(Duration.ofSeconds(5)),
                            StreamOffset.create(QuizRedisCacheConstants.TASK_STREAM_KEY, ReadOffset.lastConsumed())
                    );
            if (records == null || records.isEmpty()) {
                return;
            }
            for (MapRecord<String, Object, Object> record : records) {
                try {
                    processRecord(record);
                } catch (Exception e) {
                    log.error("[Consumer] 处理单条消息异常 entryId={}", record.getId().getValue(), e);
                }
            }
        } catch (Exception e) {
            log.error("[Consumer] 轮询 Stream 异常", e);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String entryId = record.getId().getValue();
        Map<Object, Object> fields = record.getValue();
        Object payloadObj = fields.get("payload");
        if (payloadObj == null) {
            log.warn("[Consumer] Stream 消息缺少 payload entryId={}", entryId);
            ack(entryId);
            return;
        }

        AiImportTaskMetaVO meta;
        try {
            meta = objectMapper.readValue(payloadObj.toString(), AiImportTaskMetaVO.class);
        } catch (JsonProcessingException e) {
            log.error("[Consumer] 反序列化元数据失败 entryId={}", entryId, e);
            ack(entryId);
            return;
        }

        String taskId = meta.getTaskId();
        log.info("[Consumer] 认领任务 taskId={} bankId={} type={}", taskId, meta.getBankId(), meta.getType());

        try {
            // 1. 状态 → PROCESSING
            statusStore.write(taskId, AiImportTaskStatus.PROCESSING, null, null);

            // 2. 获取待解析文本
            String rawText = resolveRawText(meta);
            if (rawText == null || rawText.isBlank()) {
                statusStore.markFailed(taskId, "解析后文本内容为空");
                ack(entryId);
                return;
            }

            // 3. 调用大模型
            String systemPrompt = systemPromptProvider.getSystemPrompt();
            if (systemPrompt == null || systemPrompt.isBlank()) {
                statusStore.markFailed(taskId, "系统提示词为空");
                ack(entryId);
                return;
            }

            String assistantText = llmChatClient.chatCompletion(systemPrompt, rawText);
            String jsonPayload = LlmJsonPayloadSanitizer.stripMarkdownCodeFence(assistantText);

            if (!jsonPayload.startsWith("[")) {
                statusStore.markFailed(taskId, "大模型输出非 JSON 数组格式");
                log.error("[Consumer] LLM 输出非数组 taskId={} preview={}", taskId,
                        jsonPayload.length() > 400 ? jsonPayload.substring(0, 400) + "..." : jsonPayload);
                ack(entryId);
                return;
            }

            // 4. 解析为预览 VO
            List<LlmQuestionParseDTO> parsed;
            try {
                parsed = objectMapper.readValue(jsonPayload, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                statusStore.markFailed(taskId, "大模型结果 JSON 反序列化失败");
                log.error("[Consumer] JSON 反序列化失败 taskId={}", taskId, e);
                ack(entryId);
                return;
            }

            List<QuestionPreviewVO> previews = new ArrayList<>();
            int skipped = 0;
            for (LlmQuestionParseDTO dto : parsed) {
                Optional<QuestionPreviewVO> preview = toPreviewVO(dto);
                if (preview.isPresent()) {
                    previews.add(preview.get());
                } else {
                    skipped++;
                }
            }
            if (skipped > 0) {
                log.warn("[Consumer] 跳过非法题目 taskId={} count={}", taskId, skipped);
            }

            // 5. 写入预览结果 → 状态 PARSED
            resultStore.writeQuestions(taskId, previews);
            statusStore.write(taskId, AiImportTaskStatus.PARSED,
                    previews.isEmpty() ? "解析完成但无可落库题目" : null,
                    previews.size());

            log.info("[Consumer] 任务解析完成 taskId={} totalCount={}", taskId, previews.size());

        } catch (Exception e) {
            log.error("[Consumer] 任务处理异常 taskId={}", taskId, e);
            statusStore.markFailed(taskId, "处理异常: " + shortMsg(e));
        }

        // 最终 ACK，避免阻塞队列
        ack(entryId);
    }

    private String resolveRawText(AiImportTaskMetaVO meta) throws IOException {
        if ("text".equals(meta.getType())) {
            return meta.getPlainText();
        }
        // type = file: 从本地文件系统读取并解析
        Path filePath = fileStorageService.resolvePath(meta.getFileUrl());
        String ext = extractExtension(meta.getFileName());
        byte[] content = Files.readAllBytes(filePath);
        return DocumentParseUtils.extractFromBytes(content, ext);
    }

    private Optional<QuestionPreviewVO> toPreviewVO(LlmQuestionParseDTO dto) {
        if (dto == null) return Optional.empty();
        if (!QuestionType.isValidCode(dto.getQuestionType())) return Optional.empty();
        String type = dto.getQuestionType().trim();
        if (dto.getStem() == null || dto.getStem().isBlank()) return Optional.empty();
        if (dto.getAnswer() == null || dto.getAnswer().isEmpty()) return Optional.empty();

        List<String> answers = normalizeAnswers(dto.getAnswer());
        if (answers.isEmpty()) return Optional.empty();

        List<String> options = dto.getOptions() == null
                ? new ArrayList<>() : new ArrayList<>(dto.getOptions());
        if (QuestionType.JUDGE.name().equals(type)) {
            options = List.of("正确", "错误");
            if (answers.size() != 1
                    || (!"T".equals(answers.get(0)) && !"F".equals(answers.get(0)))) {
                return Optional.empty();
            }
        } else {
            if (options.isEmpty() || options.stream().anyMatch(o -> o == null || o.isBlank()))
                return Optional.empty();
            if (!lettersInRange(options.size(), answers)) return Optional.empty();
        }

        return Optional.of(QuestionPreviewVO.builder()
                .questionType(type)
                .stem(dto.getStem().trim())
                .options(options)
                .answer(answers)
                .analysis(dto.getAnalysis() == null ? "" : dto.getAnalysis())
                .build());
    }

    // ---- 工具方法 ----

    private static List<String> normalizeAnswers(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            String t = s.trim().toUpperCase(Locale.ROOT);
            if (LETTER.matcher(t).matches()) out.add(t);
            else if ("T".equals(t) || "F".equals(t)) out.add(t);
        }
        if (out.isEmpty()) return List.of();
        if (out.size() > 1 && out.stream().allMatch(x -> LETTER.matcher(x).matches())) {
            out = new ArrayList<>(new LinkedHashSet<>(out));
            Collections.sort(out);
        }
        return out;
    }

    private static boolean lettersInRange(int optionCount, List<String> answers) {
        for (String letter : answers) {
            if (!LETTER.matcher(letter).matches()) return false;
            int idx = letter.charAt(0) - 'A';
            if (idx < 0 || idx >= optionCount) return false;
        }
        return true;
    }

    private static String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return "tmp";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "tmp";
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void ack(String entryId) {
        try {
            long count = stringRedisTemplate.opsForStream().acknowledge(
                    QuizRedisCacheConstants.TASK_STREAM_KEY,
                    QuizRedisCacheConstants.TASK_STREAM_GROUP,
                    entryId);
            if (count > 0) {
                log.debug("[Consumer] ACK 成功 entryId={}", entryId);
            }
        } catch (Exception e) {
            log.error("[Consumer] ACK 失败 entryId={}", entryId, e);
        }
    }

    private static String consumerName() {
        return "consumer-" + Thread.currentThread().getId();
    }

    private static String shortMsg(Throwable e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) return e.getMessage();
        return e.getClass().getSimpleName();
    }
}
