package cn.yanzongkeji.lawtest.question.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.domain.model.QuestionId;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionStatus;
import cn.yanzongkeji.lawtest.question.domain.model.QuestionType;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.UserQuestionProgressDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.UserPracticeQueryMapper;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.UserQuestionProgressMapper;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.AnswerSubmissionRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.SequentialProgressRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.request.SubjectiveAnswerSubmissionRequest;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.CursorPageResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.FavoriteQuestionStateResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.QuestionPracticeStatusListResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.QuestionPracticeStatusResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.SequentialProgressResponse;
import cn.yanzongkeji.lawtest.question.interfaces.rest.response.StudySummaryResponse;
import cn.yanzongkeji.lawtest.user.interfaces.rest.controller.PasskeyRegistrationController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private static final int DAILY_STUDY_GOAL = 20;
  private static final ZoneId STUDY_ZONE = ZoneId.of("Asia/Shanghai");
  private final QuestionRepository questions;
  private final UserQuestionProgressMapper questionProgress;
  private final UserPracticeQueryMapper practiceQuery;

  @PostMapping("/questions/{questionId}/answer")
  @Operation(summary = "保存当前用户的答题结果")
  public ResponseEntity<java.util.Map<String, Object>> submitAnswer(
      JwtAuthenticationToken authentication,
      @PathVariable long questionId,
      @RequestBody AnswerSubmissionRequest request) {
    requireActiveQuestion(questionId);
    long userId = currentUserId(authentication);
    questionProgress.upsert(userId, questionId, request.correct() ? "ANSWERED" : "WRONG");
    practiceQuery.recordDailyQuestion(userId, LocalDate.now(STUDY_ZONE), questionId);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of());
  }

  @PostMapping("/questions/{questionId}/subjective-answer")
  @Operation(summary = "保存当前用户的主观题答案")
  public ResponseEntity<java.util.Map<String, Object>> submitSubjectiveAnswer(
      JwtAuthenticationToken authentication,
      @PathVariable long questionId,
      @RequestBody SubjectiveAnswerSubmissionRequest request) {
    var question = requireActiveQuestion(questionId);
    if (question.type() != QuestionType.SUBJECTIVE) {
      throw new IllegalArgumentException("当前题目不是主观题");
    }
    long userId = currentUserId(authentication);
    questionProgress.upsertSubjectiveAnswer(userId, questionId, request.normalizedAnswer());
    practiceQuery.recordDailyQuestion(userId, LocalDate.now(STUDY_ZONE), questionId);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of());
  }

  @GetMapping("/study-summary")
  @Operation(summary = "读取当前用户今日学习统计")
  public ResponseEntity<StudySummaryResponse> studySummary(JwtAuthenticationToken authentication) {
    long userId = currentUserId(authentication);
    LocalDate today = LocalDate.now(STUDY_ZONE);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new StudySummaryResponse(
                practiceQuery.countDailyQuestions(userId, today),
                DAILY_STUDY_GOAL,
                consecutiveDays(practiceQuery.findStudyDates(userId), today)));
  }

  @GetMapping("/question-statuses")
  @Operation(summary = "读取当前用户对一组题目的作答状态")
  public ResponseEntity<QuestionPracticeStatusListResponse> questionStatuses(
      JwtAuthenticationToken authentication, @RequestParam List<Long> questionIds) {
    if (questionIds.isEmpty() || questionIds.size() > 100)
      throw new IllegalArgumentException("questionIds 必须在 1 到 100 之间");
    long userId = currentUserId(authentication);
    List<QuestionPracticeStatusResponse> response =
        questionProgress
            .selectList(
                new LambdaQueryWrapper<UserQuestionProgressDO>()
                    .eq(UserQuestionProgressDO::getUserId, userId)
                    .in(UserQuestionProgressDO::getQuestionId, questionIds))
            .stream()
            .map(item -> new QuestionPracticeStatusResponse(item.getQuestionId(), item.getStatus()))
            .toList();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new QuestionPracticeStatusListResponse(response));
  }

  @GetMapping("/wrong-question-ids")
  @Operation(summary = "游标分页读取错题本题目 ID")
  public ResponseEntity<CursorPageResponse<Long>> wrongQuestionIds(
      JwtAuthenticationToken authentication,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "20") int size) {
    validatePage(cursor, size);
    long userId = currentUserId(authentication);
    List<Long> ids = practiceQuery.findWrongQuestionIdsAfter(userId, cursor, size + 1);
    boolean hasNext = ids.size() > size;
    List<Long> items = hasNext ? ids.subList(0, size) : ids;
    Long nextCursor = hasNext ? items.getLast() : null;
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new CursorPageResponse<>(
                items, nextCursor, hasNext, practiceQuery.countWrongQuestionIds(userId)));
  }

  @GetMapping("/favorite-question-ids")
  @Operation(summary = "游标分页读取当前用户收藏的题目 ID")
  public ResponseEntity<CursorPageResponse<Long>> favoriteQuestionIds(
      JwtAuthenticationToken authentication,
      @RequestParam(required = false) Long cursor,
      @RequestParam(defaultValue = "20") int size) {
    validatePage(cursor, size);
    long userId = currentUserId(authentication);
    List<Long> ids = practiceQuery.findFavoriteQuestionIdsAfter(userId, cursor, size + 1);
    boolean hasNext = ids.size() > size;
    List<Long> items = hasNext ? ids.subList(0, size) : ids;
    Long nextCursor = hasNext ? items.getLast() : null;
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new CursorPageResponse<>(
                items, nextCursor, hasNext, practiceQuery.countFavoriteQuestionIds(userId)));
  }

  @GetMapping("/questions/{questionId}/favorite")
  @Operation(summary = "读取当前用户对题目的收藏状态")
  public ResponseEntity<FavoriteQuestionStateResponse> favoriteState(
      JwtAuthenticationToken authentication, @PathVariable long questionId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new FavoriteQuestionStateResponse(
                practiceQuery.isFavoriteQuestion(currentUserId(authentication), questionId)));
  }

  @PutMapping("/questions/{questionId}/favorite")
  @Operation(summary = "收藏题目")
  public ResponseEntity<Void> favoriteQuestion(
      JwtAuthenticationToken authentication, @PathVariable long questionId) {
    requireActiveQuestion(questionId);
    practiceQuery.addFavoriteQuestion(currentUserId(authentication), questionId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @DeleteMapping("/questions/{questionId}/favorite")
  @Operation(summary = "取消收藏题目")
  public ResponseEntity<Void> removeFavoriteQuestion(
      JwtAuthenticationToken authentication, @PathVariable long questionId) {
    practiceQuery.removeFavoriteQuestion(currentUserId(authentication), questionId);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @GetMapping("/sequential-progress")
  @Operation(summary = "读取顺序答题进度")
  public ResponseEntity<SequentialProgressResponse> sequentialProgress(
      JwtAuthenticationToken authentication, @RequestParam String scopeKey) {
    validateScope(scopeKey);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new SequentialProgressResponse(
                practiceQuery.findSequentialQuestionId(currentUserId(authentication), scopeKey)));
  }

  @PutMapping("/sequential-progress")
  @Operation(summary = "保存顺序答题进度")
  public ResponseEntity<java.util.Map<String, Object>> saveSequentialProgress(
      JwtAuthenticationToken authentication, @RequestBody SequentialProgressRequest request) {
    validateScope(request.scopeKey());
    var question = requireActiveQuestion(request.questionId());
    if (request.scopeKey().startsWith("BANK:")
        && question.questionBankId().value()
            != Long.parseLong(request.scopeKey().substring("BANK:".length()))) {
      throw new IllegalArgumentException("题目不属于当前题库");
    }
    practiceQuery.upsertSequentialQuestionId(
        currentUserId(authentication), request.scopeKey(), request.questionId());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(java.util.Map.of());
  }

  private cn.yanzongkeji.lawtest.question.domain.model.Question requireActiveQuestion(
      long questionId) {
    return questions
        .findById(new QuestionId(questionId))
        .filter(question -> question.status() == QuestionStatus.ACTIVE)
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

  private static int consecutiveDays(List<LocalDate> studyDates, LocalDate today) {
    int count = 0;
    LocalDate expected = today;
    for (LocalDate studyDate : studyDates) {
      if (studyDate.equals(expected)) {
        count++;
        expected = expected.minusDays(1);
      } else if (studyDate.isBefore(expected)) {
        break;
      }
    }
    return count;
  }
}
