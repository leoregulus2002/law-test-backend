package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.application.command.QuestionManagementUseCase;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionCommand;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionIdCursorPage;
import cn.yanzongkeji.lawtest.question.application.dto.QuestionPage;
import cn.yanzongkeji.lawtest.question.application.query.QuestionQueryUseCase;
import cn.yanzongkeji.lawtest.question.domain.model.Question;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.QuestionUpsertRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.QuestionStatusUpdateRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "题目管理", description = "题目的查询、新增、完整更新与删除")
public class QuestionController {
    private final QuestionQueryUseCase query;
    private final QuestionManagementUseCase commands;

    @GetMapping("/question-banks/{bankId}/questions")
    @Operation(summary = "分页查询题库题目")
    public PageResponse<QuestionDetailResponse> list(@PathVariable long bankId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        QuestionPage<Question> r = query.questions(bankId, page, size);
        return new PageResponse<>(r.items().stream().map(QuestionDetailResponse::from).toList(), r.page(), r.size(),
                r.total());
    }

    @GetMapping("/questions")
    @Operation(summary = "分页查询全部题目")
    public PageResponse<QuestionListResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        QuestionPage<cn.yanzongkeji.lawtest.question.application.dto.QuestionListItem> result = query.questions(page, size);
        return new PageResponse<>(result.items().stream().map(QuestionListResponse::from).toList(), result.page(),
                result.size(), result.total());
    }

    @GetMapping("/questions/{id}")
    @Operation(summary = "查询题目详情")
    public QuestionDetailResponse detail(@PathVariable long id) {
        return QuestionDetailResponse.from(query.question(id));
    }

    /** 按游标查询全部或指定题库范围内的题目 ID。 */
    @GetMapping("/questions/ids")
    @Operation(summary = "游标分页查询题目 ID", description = "不传 questionBankIds 查询全部题库；cursor 使用上次返回的 nextCursor")
    public CursorPageResponse<Long> ids(@RequestParam(required = false) java.util.List<Long> questionBankIds,
            @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "100") int size) {
        QuestionIdCursorPage result = query.questionIds(questionBankIds, cursor, size);
        return new CursorPageResponse<>(result.items(), result.nextCursor(), result.hasNext(), result.total());
    }

    /** 随机查询全部或指定题库范围内的一个题目 ID。 */
    @GetMapping("/questions/random-id")
    @Operation(summary = "随机查询题目 ID", description = "不传 questionBankIds 查询全部题库；可传一个或多个题库 ID")
    public QuestionIdResponse randomId(@RequestParam(required = false) java.util.List<Long> questionBankIds) {
        return new QuestionIdResponse(query.randomQuestionId(questionBankIds));
    }

    @PostMapping("/question-banks/{bankId}/questions")
    @Operation(summary = "新增题目")
    public ResponseEntity<QuestionDetailResponse> create(@PathVariable long bankId,
            @RequestBody QuestionUpsertRequest request) {
        Question q = commands.create(bankId, toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(QuestionDetailResponse.from(q));
    }

    @PutMapping("/questions/{id}")
    @Operation(summary = "完整更新题目")
    public QuestionDetailResponse replace(@PathVariable long id, @RequestBody QuestionUpsertRequest request) {
        return QuestionDetailResponse.from(commands.replace(id, toCommand(request)));
    }

    @PatchMapping("/questions/{id}/status")
    @Operation(summary = "更新题目状态")
    public QuestionDetailResponse changeStatus(@PathVariable long id, @RequestBody QuestionStatusUpdateRequest request) {
        return QuestionDetailResponse.from(commands.changeStatus(id, request.status()));
    }

    @DeleteMapping("/questions/{id}")
    @Operation(summary = "删除题目", description = "同步删除该题目的选项和答案")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        commands.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }

    private static QuestionCommand toCommand(QuestionUpsertRequest r) {
        return new QuestionCommand(r.number(), r.stem(),
                r.options() == null ? java.util.List.of()
                        : r.options().stream().map(o -> new QuestionCommand.Option(o.label(), o.content())).toList(),
                r.answers() == null ? java.util.List.of() : r.answers(), r.questionType(), r.analysis());
    }
}
