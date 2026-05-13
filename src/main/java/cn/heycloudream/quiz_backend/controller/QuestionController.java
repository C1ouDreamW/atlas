package cn.heycloudream.quiz_backend.controller;

import cn.heycloudream.quiz_backend.util.UserContextHolder;
import cn.heycloudream.quiz_backend.common.vo.Result;
import cn.heycloudream.quiz_backend.dto.question.QuestionUpdateDTO;
import cn.heycloudream.quiz_backend.service.QuestionService;
import cn.heycloudream.quiz_backend.vo.question.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 试题 REST 接口：详情查询、全量更新、删除（资源路径以试题为主键）。
 *
 * @author atlas
 */
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Validated
@Tag(name = "题库管理", description = "试题详情、更新与删除")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/{id}")
    @Operation(summary = "根据试题 ID 获取详情", description = "仅所属题库所有者可以查看。")
    public Result<QuestionVO> getById(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.get();
        return Result.success(questionService.getQuestionById(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "全量更新试题", description = "不可更换所属题库；仅所属题库所有者可更新。")
    public Result<Void> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody QuestionUpdateDTO dto) {
        Long userId = UserContextHolder.get();
        questionService.updateQuestion(userId, id, dto);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除试题", description = "逻辑删除；仅所属题库所有者可删除。")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.get();
        questionService.deleteQuestion(userId, id);
        return Result.success(null);
    }
}