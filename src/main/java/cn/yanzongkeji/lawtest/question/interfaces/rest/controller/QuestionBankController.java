package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.command.QuestionManagementUseCase;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;
import cn.yanzongkeji.lawtest.question.application.query.QuestionQueryUseCase;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/question-banks")
@Tag(name = "题库管理", description = "查询和删除题库")
public class QuestionBankController {
    private final QuestionQueryUseCase query;
    private final QuestionManagementUseCase commands;

    @GetMapping
    @Operation(summary = "分页查询题库")
    public PageResponse<QuestionBankResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        QuestionPage<?> result = query.questionBanks(page, size);
        return new PageResponse<>(result.items().stream()
                .map(x -> QuestionBankResponse.from((cn.yanzongkeji.lawtest.question.domain.model.QuestionBank) x))
                .toList(), result.page(), result.size(), result.total());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询题库详情")
    public QuestionBankResponse detail(@PathVariable long id) {
        return QuestionBankResponse.from(query.questionBank(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除题库", description = "同步删除该题库中的题目、选项和答案")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        commands.deleteQuestionBank(id);
        return ResponseEntity.noContent().build();
    }
}
