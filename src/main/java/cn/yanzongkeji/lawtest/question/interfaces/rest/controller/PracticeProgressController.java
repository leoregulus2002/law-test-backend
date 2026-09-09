package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.UserQuestionProgressDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.UserPracticeQueryMapper;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.UserQuestionProgressMapper;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.AnswerSubmissionRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.SequentialProgressRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.CursorPageResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.QuestionPracticeStatusResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.QuestionPracticeStatusListResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.SequentialProgressResponse;
import cn.yanzongkeji.lawtest.user.interfaces.rest.controller.PasskeyRegistrationController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/practice")
public class PracticeProgressController {
    private final QuestionRepository questions;
    private final UserQuestionProgressMapper questionProgress;
    private final UserPracticeQueryMapper practiceQuery;

    @PostMapping("/questions/{questionId}/answer")
    @Operation(summary = "保存当前用户的答题结果")
    public ResponseEntity<java.util.Map<String, Object>> submitAnswer(JwtAuthenticationToken authentication, @PathVariable long questionId,
            @RequestBody AnswerSubmissionRequest request) {
        requireActiveQuestion(questionId);
        questionProgress.upsert(currentUserId(authentication), questionId, request.correct() ? "ANSWERED" : "WRONG");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of());
    }

    @GetMapping("/question-statuses")
    @Operation(summary = "读取当前用户对一组题目的作答状态")
    public ResponseEntity<QuestionPracticeStatusListResponse> questionStatuses(JwtAuthenticationToken authentication,
            @RequestParam List<Long> questionIds) {
        if (questionIds.isEmpty() || questionIds.size() > 100) throw new IllegalArgumentException("questionIds 必须在 1 到 100 之间");
        long userId = currentUserId(authentication);
        List<QuestionPracticeStatusResponse> response = questionProgress.selectList(new LambdaQueryWrapper<UserQuestionProgressDO>()
                .eq(UserQuestionProgressDO::getUserId, userId).in(UserQuestionProgressDO::getQuestionId, questionIds))
                .stream().map(item -> new QuestionPracticeStatusResponse(item.getQuestionId(), item.getStatus())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new QuestionPracticeStatusListResponse(response));
    }

    @GetMapping("/wrong-question-ids")
    @Operation(summary = "游标分页读取错题本题目 ID")
    public ResponseEntity<CursorPageResponse<Long>> wrongQuestionIds(JwtAuthenticationToken authentication,
            @RequestParam(required = false) Long cursor, @RequestParam(defaultValue = "20") int size) {
        validatePage(cursor, size);
        long userId = currentUserId(authentication);
        List<Long> ids = practiceQuery.findWrongQuestionIdsAfter(userId, cursor, size + 1);
        boolean hasNext = ids.size() > size;
        List<Long> items = hasNext ? ids.subList(0, size) : ids;
        Long nextCursor = hasNext ? items.getLast() : null;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CursorPageResponse<>(items, nextCursor,
                hasNext, practiceQuery.countWrongQuestionIds(userId)));
    }

    @GetMapping("/sequential-progress")
    @Operation(summary = "读取顺序答题进度")
    public ResponseEntity<SequentialProgressResponse> sequentialProgress(JwtAuthenticationToken authentication,
            @RequestParam String scopeKey) {
        validateScope(scopeKey);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SequentialProgressResponse(practiceQuery.findSequentialQuestionId(currentUserId(authentication), scopeKey)));
    }

    @PutMapping("/sequential-progress")
    @Operation(summary = "保存顺序答题进度")
    public ResponseEntity<java.util.Map<String, Object>> saveSequentialProgress(JwtAuthenticationToken authentication,
            @RequestBody SequentialProgressRequest request) {
        validateScope(request.scopeKey());
        var question = requireActiveQuestion(request.questionId());
        if (request.scopeKey().startsWith("BANK:")
                && question.questionBankId().value() != Long.parseLong(request.scopeKey().substring("BANK:".length()))) {
            throw new IllegalArgumentException("题目不属于当前题库");
        }
        practiceQuery.upsertSequentialQuestionId(currentUserId(authentication), request.scopeKey(), request.questionId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of());
    }

    private cn.yanzongkeji.lawtest.question.domain.model.Question requireActiveQuestion(long questionId) {
        return questions.findById(new QuestionId(questionId)).filter(question -> question.status() == QuestionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在或不可练习"));
    }

    private static long currentUserId(JwtAuthenticationToken authentication) {
        return PasskeyRegistrationController.currentUser(authentication).value();
    }

    private static void validateScope(String scopeKey) {
        if (scopeKey == null || !(scopeKey.equals("ALL") || scopeKey.matches("BANK:[1-9][0-9]*"))) {
            throw new IllegalArgumentException("无效的练习范围");
        }
    }

    private static void validatePage(Long cursor, int size) {
        if (cursor != null && cursor <= 0) throw new IllegalArgumentException("cursor 必须为正整数");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size 必须在 1 到 100 之间");
    }
}
