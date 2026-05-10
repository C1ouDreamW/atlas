package cn.heycloudream.quiz_backend.service.impl;

import cn.heycloudream.quiz_backend.client.LlmChatClient;
import cn.heycloudream.quiz_backend.config.AsyncConfig;
import cn.heycloudream.quiz_backend.dto.ai.LlmQuestionParseDTO;
import cn.heycloudream.quiz_backend.entity.Question;
import cn.heycloudream.quiz_backend.enums.QuestionType;
import cn.heycloudream.quiz_backend.exception.LlmInvokeException;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.service.ai.AiQuestionImportRedisStatusStore;
import cn.heycloudream.quiz_backend.service.prompt.AiQuestionImportSystemPromptProvider;
import cn.heycloudream.quiz_backend.util.LlmJsonPayloadSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 智能导入异步执行器：调用大模型、Jackson 反序列化、校验映射、MyBatis-Plus 批量落库。
 * <p>
 * 与 {@link AiQuestionImportServiceImpl} 分离，确保 {@code @Async} 经 Spring 代理生效。
 * 任务状态写入 Redis（Key: {@code smart_quiz:import_status:{bankId}}），供前端轮询。
 * </p>
 *
 * @author atlas
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiQuestionImportAsyncProcessor {

    private static final Pattern LETTER = Pattern.compile("[A-Z]");

    /**
     * 针对 MySql 中大对象或者 longtext 的数据保护长度截断，避免单个超大脏数据引发 SQL 包过大。
     */
    private static final int MAX_RAW_LLM_JSON_LENGTH = 16_384;

    private final LlmChatClient llmChatClient;
    private final ObjectMapper objectMapper;
    private final QuestionService questionService;
    private final AiQuestionImportSystemPromptProvider systemPromptProvider;
    private final AiQuestionImportRedisStatusStore importStatusStore;

    /**
     * 异步执行导入链路；异常在异步线程内捕获并落 Redis，不向调用方抛出。
     */
    @Async(AsyncConfig.AI_IMPORT_EXECUTOR)
    public void processAsync(Long questionBankId, String rawText) {
        importStatusStore.writeProcessing(questionBankId);
        try {
            runImport(questionBankId, rawText);
        } catch (LlmInvokeException e) {
            String detail = e.getMessage() == null ? "未知错误" : e.getMessage();
            importStatusStore.writeFailed(questionBankId, "调用 AI 失败：" + detail);
            log.error("[BankId:{}] AI 题库导入异步任务：大模型调用失败", questionBankId, e);
        } catch (Exception e) {
            importStatusStore.writeFailed(questionBankId, "导入失败：" + shortCauseMessage(e));
            log.error("[BankId:{}] AI 题库导入异步任务未预期异常", questionBankId, e);
        }
    }

    private void runImport(Long questionBankId, String rawText) {
        String systemPrompt = systemPromptProvider.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.isBlank()) {
            importStatusStore.writeFailed(questionBankId, "系统提示词为空，无法调用大模型");
            log.error("[BankId:{}] 系统提示词为空，已终止导入", questionBankId);
            return;
        }
        String assistantText = llmChatClient.chatCompletion(systemPrompt, rawText);
        String jsonPayload = LlmJsonPayloadSanitizer.stripMarkdownCodeFence(assistantText);
        if (!jsonPayload.startsWith("[")) {
            importStatusStore.writeFailed(questionBankId, "大模型输出非 JSON 数组格式，已放弃落库");
            log.error("[BankId:{}] 大模型输出非 JSON 数组开头，已放弃落库 preview={}",
                    questionBankId, preview(jsonPayload, 400));
            return;
        }
        List<LlmQuestionParseDTO> parsed;
        try {
            parsed = objectMapper.readValue(jsonPayload, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            importStatusStore.writeFailed(questionBankId, "大模型结果 JSON 反序列化失败");
            log.error("[BankId:{}] 大模型 JSON 反序列化失败 preview={}",
                    questionBankId, preview(jsonPayload, 800), e);
            return;
        }
        if (parsed == null || parsed.isEmpty()) {
            importStatusStore.writeSuccess(questionBankId, 0, "大模型返回空数组，未识别到有效试题");
            log.info("[BankId:{}] 大模型返回空数组，未识别到有效试题", questionBankId);
            return;
        }
        List<Question> toSave = new ArrayList<>();
        int skipped = 0;
        int order = 1;
        for (LlmQuestionParseDTO dto : parsed) {
            Optional<Question> q = toQuestionEntity(dto, questionBankId, order);
            if (q.isPresent()) {
                toSave.add(q.get());
                order++;
            } else {
                skipped++;
            }
        }
        if (skipped > 0) {
            log.warn("[BankId:{}] AI 导入跳过非法题目条数={}", questionBankId, skipped);
        }
        if (toSave.isEmpty()) {
            importStatusStore.writeSuccess(questionBankId, 0, "校验后无有效题目可落库");
            log.info("[BankId:{}] 校验后无有效题目可落库", questionBankId);
            return;
        }
        questionService.saveImportedQuestions(toSave);
        importStatusStore.writeSuccess(questionBankId, toSave.size(), null);
        log.info("[BankId:{}] AI 题库导入完成 saved={}", questionBankId, toSave.size());
    }

    private Optional<Question> toQuestionEntity(LlmQuestionParseDTO dto, Long questionBankId, int sortNo) {
        if (dto == null) {
            return Optional.empty();
        }
        if (!QuestionType.isValidCode(dto.getQuestionType())) {
            return Optional.empty();
        }
        String type = dto.getQuestionType().trim();
        if (dto.getStem() == null || dto.getStem().isBlank()) {
            return Optional.empty();
        }
        if (dto.getAnswer() == null || dto.getAnswer().isEmpty()) {
            return Optional.empty();
        }
        List<String> answers = normalizeAnswers(dto.getAnswer());
        if (answers.isEmpty()) {
            return Optional.empty();
        }
        List<String> options = dto.getOptions() == null ? new ArrayList<>() : new ArrayList<>(dto.getOptions());
        if (QuestionType.JUDGE.name().equals(type)) {
            options = List.of("正确", "错误");
            if (!isValidJudgeAnswers(answers)) {
                return Optional.empty();
            }
        } else {
            if (options.isEmpty() || options.stream().anyMatch(o -> o == null || o.isBlank())) {
                return Optional.empty();
            }
            if (!lettersInRange(options.size(), answers)) {
                return Optional.empty();
            }
            if (QuestionType.SINGLE.name().equals(type) && answers.size() != 1) {
                return Optional.empty();
            }
            if (QuestionType.MULTI.name().equals(type) && answers.size() < 1) {
                return Optional.empty();
            }
        }
        String optionsJson;
        String answerJson;
        String rawSnippet;
        try {
            optionsJson = objectMapper.writeValueAsString(options);
            answerJson = objectMapper.writeValueAsString(answers);
            rawSnippet = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.warn("[BankId:{}] 题目序列化为 JSON 字段失败，已跳过 sortNo={}", questionBankId, sortNo, e);
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        String analysis = dto.getAnalysis() == null ? "" : dto.getAnalysis();
        Question entity = Question.builder()
                .questionBankId(questionBankId)
                .questionType(type)
                .stem(dto.getStem().trim())
                .optionsJson(optionsJson)
                .answerJson(answerJson)
                .analysis(analysis)
                .rawLlmJson(truncate(rawSnippet, MAX_RAW_LLM_JSON_LENGTH))
                .sortNo(sortNo)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();
        return Optional.of(entity);
    }

    private static List<String> normalizeAnswers(List<String> raw) {
        List<String> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) {
                continue;
            }
            String t = s.trim().toUpperCase(Locale.ROOT);
            if (LETTER.matcher(t).matches()) {
                out.add(t);
            } else if ("T".equals(t) || "F".equals(t)) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        if (out.size() > 1 && out.stream().allMatch(x -> LETTER.matcher(x).matches())) {
            out = new ArrayList<>(new LinkedHashSet<>(out));
            Collections.sort(out);
        }
        return out;
    }

    private static boolean isValidJudgeAnswers(List<String> answers) {
        if (answers.size() != 1) {
            return false;
        }
        String a = answers.get(0);
        return "T".equals(a) || "F".equals(a);
    }

    private static boolean lettersInRange(int optionCount, List<String> answers) {
        for (String letter : answers) {
            if (!LETTER.matcher(letter).matches()) {
                return false;
            }
            int idx = letter.charAt(0) - 'A';
            if (idx < 0 || idx >= optionCount) {
                return false;
            }
        }
        return true;
    }

    private static String preview(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r", " ").replace("\n", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "...";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String shortCauseMessage(Throwable e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }
}
