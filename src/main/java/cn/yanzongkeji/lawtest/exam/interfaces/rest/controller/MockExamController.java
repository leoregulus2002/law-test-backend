package cn.yanzongkeji.lawtest.exam.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.dataobject.*;
import cn.yanzongkeji.lawtest.exam.infrastructure.persistence.mapper.*;
import cn.yanzongkeji.lawtest.exam.interfaces.rest.request.*;
import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.QuestionMapper;
import cn.yanzongkeji.lawtest.user.interfaces.rest.controller.PasskeyRegistrationController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** 模拟考试的组卷、作答与自动评分接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MockExamController {
  private final MockExamMapper exams;
  private final MockExamBankMapper examBanks;
  private final MockExamRuleMapper rules;
  private final MockExamAttemptMapper attempts;
  private final MockExamAttemptQuestionMapper attemptQuestions;
  private final QuestionMapper questionMapper;
  private final QuestionRepository questionRepository;

  @GetMapping("/admin/exams")
  @Operation(summary = "后台查询模拟考试")
  public List<Map<String, Object>> adminList() {
    return exams
        .selectList(new LambdaQueryWrapper<MockExamDO>().orderByDesc(MockExamDO::getId))
        .stream()
        .map(this::examSummary)
        .toList();
  }

  @PostMapping("/admin/exams")
  @Transactional
  @Operation(summary = "创建模拟考试")
  public ResponseEntity<Map<String, Object>> create(@RequestBody MockExamUpsertRequest request) {
    MockExamDO exam = new MockExamDO();
    applyExam(exam, request);
    exam.setStatus("DRAFT");
    exams.insert(exam);
    saveStructure(exam.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(examSummary(exam));
  }

  @PutMapping("/admin/exams/{examId}")
  @Transactional
  @Operation(summary = "更新草稿模拟考试")
  public Map<String, Object> update(
      @PathVariable long examId, @RequestBody MockExamUpsertRequest request) {
    MockExamDO exam = requireExam(examId);
    if (!"DRAFT".equals(exam.getStatus())) throw new IllegalArgumentException("只能编辑草稿试卷");
    applyExam(exam, request);
    exams.updateById(exam);
    examBanks.delete(
        new LambdaQueryWrapper<MockExamBankDO>().eq(MockExamBankDO::getExamId, examId));
    rules.delete(new LambdaQueryWrapper<MockExamRuleDO>().eq(MockExamRuleDO::getExamId, examId));
    saveStructure(examId, request);
    return examSummary(exam);
  }

  @PostMapping("/admin/exams/{examId}/publish")
  @Transactional
  @Operation(summary = "发布模拟考试")
  public Map<String, Object> publish(@PathVariable long examId) {
    MockExamDO exam = requireExam(examId);
    if (!"DRAFT".equals(exam.getStatus())) throw new IllegalArgumentException("该试卷不能重复发布");
    validateAvailability(examId);
    exam.setStatus("PUBLISHED");
    exam.setPublishedAt(Instant.now());
    exams.updateById(exam);
    return examSummary(exam);
  }

  @GetMapping("/exams")
  @Operation(summary = "查询可参加的模拟考试")
  public List<Map<String, Object>> list() {
    return exams
        .selectList(
            new LambdaQueryWrapper<MockExamDO>()
                .eq(MockExamDO::getStatus, "PUBLISHED")
                .orderByDesc(MockExamDO::getPublishedAt))
        .stream()
        .map(this::examSummary)
        .toList();
  }

  @PutMapping("/admin/mock-exam-configuration")
  @Transactional
  @Operation(summary = "保存模拟考试参数")
  public Map<String, Object> saveConfiguration(@RequestBody MockExamStartRequest request) {
    MockExamUpsertRequest configuration = toConfiguration(request);
    MockExamDO exam = new MockExamDO();
    applyExam(exam, configuration);
    validateConfigurationAvailability(configuration);
    exam.setStatus("DRAFT");
    exams.insert(exam);
    saveStructure(exam.getId(), configuration);
    return examSummary(exam);
  }

  @GetMapping("/admin/mock-exam-configurations")
  @Operation(summary = "查询模拟考试参数列表")
  public List<Map<String, Object>> configurationList() {
    return exams
        .selectList(
            new LambdaQueryWrapper<MockExamDO>()
                .in(MockExamDO::getStatus, List.of("DRAFT", "PUBLISHED"))
                .orderByDesc(MockExamDO::getId))
        .stream()
        .map(this::examSummary)
        .toList();
  }

  @PostMapping("/admin/mock-exam-configurations/{examId}/activate")
  @Transactional
  @Operation(summary = "启用一套模拟考试参数")
  public Map<String, Object> activateConfiguration(@PathVariable long examId) {
    MockExamDO exam = requireExam(examId);
    if (!"DRAFT".equals(exam.getStatus()) && !"PUBLISHED".equals(exam.getStatus()))
      throw new IllegalArgumentException("该模拟考试参数不能启用");
    validateAvailability(examId);
    exams.update(
        new LambdaUpdateWrapper<MockExamDO>()
            .eq(MockExamDO::getStatus, "PUBLISHED")
            .ne(MockExamDO::getId, examId)
            .set(MockExamDO::getStatus, "DRAFT"));
    exam.setStatus("PUBLISHED");
    exam.setPublishedAt(Instant.now());
    exams.updateById(exam);
    return examSummary(exam);
  }

  @PostMapping("/admin/mock-exam-configurations/{examId}/deactivate")
  @Transactional
  @Operation(summary = "停用一套模拟考试参数")
  public Map<String, Object> deactivateConfiguration(@PathVariable long examId) {
    MockExamDO exam = requireExam(examId);
    if (!"PUBLISHED".equals(exam.getStatus())) throw new IllegalArgumentException("该模拟考试参数当前未启用");
    exam.setStatus("DRAFT");
    exams.updateById(exam);
    return examSummary(exam);
  }

  @PutMapping("/admin/mock-exam-configurations/{examId}")
  @Transactional
  @Operation(summary = "编辑模拟考试参数")
  public Map<String, Object> updateConfiguration(
      @PathVariable long examId, @RequestBody MockExamStartRequest request) {
    MockExamDO exam = requireExam(examId);
    if ("ARCHIVED".equals(exam.getStatus())) throw new IllegalArgumentException("已删除的模拟考试参数不能编辑");
    MockExamUpsertRequest configuration = toConfiguration(request);
    applyExam(exam, configuration);
    validateConfigurationAvailability(configuration);
    exams.updateById(exam);
    examBanks.delete(
        new LambdaQueryWrapper<MockExamBankDO>().eq(MockExamBankDO::getExamId, examId));
    rules.delete(new LambdaQueryWrapper<MockExamRuleDO>().eq(MockExamRuleDO::getExamId, examId));
    saveStructure(examId, configuration);
    return examSummary(exam);
  }

  @DeleteMapping("/admin/mock-exam-configurations/{examId}")
  @Transactional
  @Operation(summary = "删除模拟考试参数")
  public ResponseEntity<Void> deleteConfiguration(@PathVariable long examId) {
    MockExamDO exam = requireExam(examId);
    exam.setStatus("ARCHIVED");
    exams.updateById(exam);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/mock-exam-configuration")
  @Operation(summary = "读取当前模拟考试参数")
  public Map<String, Object> configuration() {
    return examSummary(activeConfiguration());
  }

  @PostMapping("/exams/{examId}/attempts")
  @Transactional
  @Operation(summary = "开始模拟考试", description = "首次请求随机组卷并固化题目，之后返回未交卷的同一场考试")
  public Map<String, Object> start(
      JwtAuthenticationToken authentication, @PathVariable long examId) {
    MockExamDO exam = requirePublishedExam(examId);
    long userId = PasskeyRegistrationController.currentUser(authentication).value();
    MockExamAttemptDO active =
        attempts.selectOne(
            new LambdaQueryWrapper<MockExamAttemptDO>()
                .eq(MockExamAttemptDO::getExamId, examId)
                .eq(MockExamAttemptDO::getUserId, userId)
                .eq(MockExamAttemptDO::getStatus, "IN_PROGRESS")
                .orderByDesc(MockExamAttemptDO::getId)
                .last("limit 1"));
    if (active != null) return attemptView(active, false);
    List<MockExamAttemptQuestionDO> generated = generateQuestions(examId);
    MockExamAttemptDO attempt = new MockExamAttemptDO();
    attempt.setExamId(examId);
    attempt.setUserId(userId);
    attempt.setStartedAt(Instant.now());
    attempt.setExpiresAt(
        attempt.getStartedAt().plus(Duration.ofMinutes(exam.getDurationMinutes())));
    attempt.setStatus("IN_PROGRESS");
    attempts.insert(attempt);
    for (MockExamAttemptQuestionDO item : generated) {
      item.setAttemptId(attempt.getId());
      attemptQuestions.insert(item);
    }
    return attemptView(attempt, false);
  }

  @PostMapping("/mock-exams/attempts")
  @Transactional
  @Operation(summary = "按当前参数开始一场新的模拟考试")
  public Map<String, Object> startCustom(JwtAuthenticationToken authentication) {
    MockExamDO exam = activeConfiguration();
    return createAttempt(exam, PasskeyRegistrationController.currentUser(authentication).value());
  }

  @PutMapping("/exam-attempts/{attemptId}/questions/{orderNo}/answer")
  @Transactional
  @Operation(summary = "自动保存模拟考试作答")
  public ResponseEntity<Void> saveAnswer(
      JwtAuthenticationToken authentication,
      @PathVariable long attemptId,
      @PathVariable int orderNo,
      @RequestBody MockExamAnswerRequest request) {
    MockExamAttemptDO attempt = requireOwnedAttempt(authentication, attemptId);
    requireOpen(attempt);
    MockExamAttemptQuestionDO item = requireAttemptQuestion(attemptId, orderNo);
    if ("SUBJECTIVE".equals(item.getQuestionType())) {
      attemptQuestions.update(
          new LambdaUpdateWrapper<MockExamAttemptQuestionDO>()
              .eq(MockExamAttemptQuestionDO::getId, item.getId())
              .set(
                  MockExamAttemptQuestionDO::getSubjectiveAnswer,
                  trim(request.subjectiveAnswer())));
    } else {
      String answer =
          request.selectedAnswers() == null
              ? ""
              : request.selectedAnswers().stream()
                  .filter(Objects::nonNull)
                  .map(String::strip)
                  .map(String::toUpperCase)
                  .sorted()
                  .reduce((a, b) -> a + "," + b)
                  .orElse("");
      attemptQuestions.update(
          new LambdaUpdateWrapper<MockExamAttemptQuestionDO>()
              .eq(MockExamAttemptQuestionDO::getId, item.getId())
              .set(MockExamAttemptQuestionDO::getSelectedAnswer, answer));
    }
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/exam-attempts/{attemptId}/submit")
  @Transactional
  @Operation(summary = "交卷并自动评分")
  public Map<String, Object> submit(
      JwtAuthenticationToken authentication, @PathVariable long attemptId) {
    MockExamAttemptDO attempt = requireOwnedAttempt(authentication, attemptId);
    if ("SUBMITTED".equals(attempt.getStatus())) return attemptView(attempt, true);
    score(attempt);
    return attemptView(requireAttempt(attemptId), true);
  }

  @GetMapping("/exam-attempts/{attemptId}")
  @Operation(summary = "读取考试进度或成绩")
  public Map<String, Object> attempt(
      JwtAuthenticationToken authentication, @PathVariable long attemptId) {
    MockExamAttemptDO value = requireOwnedAttempt(authentication, attemptId);
    return attemptView(value, "SUBMITTED".equals(value.getStatus()));
  }

  private void applyExam(MockExamDO exam, MockExamUpsertRequest r) {
    if (r.title() == null || r.title().isBlank()) throw new IllegalArgumentException("试卷名称不能为空");
    if (r.durationMinutes() <= 0) throw new IllegalArgumentException("考试时长必须大于 0");
    if (r.passingScore() == null || r.passingScore().signum() < 0)
      throw new IllegalArgumentException("及格分必须大于等于 0");
    BigDecimal total = validateRules(r.rules(), r.questionBankIds());
    if (r.passingScore().compareTo(total) > 0) throw new IllegalArgumentException("及格分不能超过试卷总分");
    if (r.questionBankIds() == null || r.questionBankIds().isEmpty())
      throw new IllegalArgumentException("至少选择一个题库");
    exam.setTitle(r.title().strip());
    exam.setDurationMinutes(r.durationMinutes());
    exam.setPassingScore(r.passingScore());
  }

  private MockExamUpsertRequest toConfiguration(MockExamStartRequest request) {
    return new MockExamUpsertRequest(
        request.title() == null || request.title().isBlank() ? "模拟考试" : request.title(),
        request.durationMinutes(),
        request.passingScore(),
        request.questionBankIds(),
        request.rules());
  }

  private void validateConfigurationAvailability(MockExamUpsertRequest request) {
    for (MockExamRuleRequest rule : request.rules()) {
      int available = candidateCount(request.questionBankIds(), rule.questionType());
      if ("RANDOM".equals(rule.selectionMode()) && available < rule.questionCount())
        throw new IllegalArgumentException(rule.questionType() + " 可用题目不足");
      if ("ALL".equals(rule.selectionMode()) && available == 0)
        throw new IllegalArgumentException(rule.questionType() + " 没有可用题目");
    }
  }

  private Map<String, Object> createAttempt(MockExamDO exam, long userId) {
    List<MockExamAttemptQuestionDO> generated = generateQuestions(exam.getId());
    MockExamAttemptDO attempt = new MockExamAttemptDO();
    attempt.setExamId(exam.getId());
    attempt.setUserId(userId);
    attempt.setStartedAt(Instant.now());
    attempt.setExpiresAt(
        attempt.getStartedAt().plus(Duration.ofMinutes(exam.getDurationMinutes())));
    attempt.setStatus("IN_PROGRESS");
    attempts.insert(attempt);
    for (MockExamAttemptQuestionDO item : generated) {
      item.setAttemptId(attempt.getId());
      attemptQuestions.insert(item);
    }
    return attemptView(attempt, false);
  }

  private BigDecimal validateRules(List<MockExamRuleRequest> input, List<Long> questionBankIds) {
    if (input == null || input.isEmpty()) throw new IllegalArgumentException("至少设置一种题型");
    Set<String> seen = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (MockExamRuleRequest r : input) {
      if (r == null || !seen.add(r.questionType()) || !isType(r.questionType()))
        throw new IllegalArgumentException("题型配置无效或重复");
      if (!("RANDOM".equals(r.selectionMode()) || "ALL".equals(r.selectionMode())))
        throw new IllegalArgumentException("抽题方式无效");
      if (r.questionCount() < 0 || ("RANDOM".equals(r.selectionMode()) && r.questionCount() == 0))
        throw new IllegalArgumentException("随机抽题数量必须大于 0");
      if (r.score() == null || r.score().signum() <= 0)
        throw new IllegalArgumentException("每题分数必须大于 0");
      int count =
          "ALL".equals(r.selectionMode())
              ? candidateCount(questionBankIds, r.questionType())
              : r.questionCount();
      total = total.add(r.score().multiply(BigDecimal.valueOf(count)));
    }
    return total;
  }

  private void saveStructure(long examId, MockExamUpsertRequest request) {
    for (Long bankId : new LinkedHashSet<>(request.questionBankIds())) {
      if (bankId == null || bankId <= 0) throw new IllegalArgumentException("题库无效");
      MockExamBankDO item = new MockExamBankDO();
      item.setExamId(examId);
      item.setQuestionBankId(bankId);
      examBanks.insert(item);
    }
    for (MockExamRuleRequest r : request.rules()) {
      MockExamRuleDO item = new MockExamRuleDO();
      item.setExamId(examId);
      item.setQuestionType(r.questionType());
      item.setSelectionMode(r.selectionMode());
      item.setQuestionCount(r.questionCount());
      item.setScore(r.score());
      rules.insert(item);
    }
  }

  private void validateAvailability(long examId) {
    for (MockExamRuleDO rule : examRules(examId)) {
      int available = candidates(examId, rule.getQuestionType()).size();
      if ("RANDOM".equals(rule.getSelectionMode()) && available < rule.getQuestionCount())
        throw new IllegalArgumentException(
            rule.getQuestionType()
                + " 可用题目不足，需要 "
                + rule.getQuestionCount()
                + " 道，当前仅 "
                + available
                + " 道");
      if ("ALL".equals(rule.getSelectionMode()) && available == 0)
        throw new IllegalArgumentException(rule.getQuestionType() + " 没有可用题目");
    }
  }

  private List<MockExamAttemptQuestionDO> generateQuestions(long examId) {
    validateAvailability(examId);
    List<MockExamAttemptQuestionDO> generated = new ArrayList<>();
    int order = 1;
    for (MockExamRuleDO rule : examRules(examId)) {
      List<QuestionDO> pool = candidates(examId, rule.getQuestionType());
      Collections.shuffle(pool);
      int count = "ALL".equals(rule.getSelectionMode()) ? pool.size() : rule.getQuestionCount();
      for (QuestionDO question : pool.subList(0, count)) {
        Question full = questionRepository.findById(new QuestionId(question.getId())).orElseThrow();
        MockExamAttemptQuestionDO item = new MockExamAttemptQuestionDO();
        item.setQuestionId(question.getId());
        item.setOrderNo(order++);
        item.setQuestionType(rule.getQuestionType());
        item.setScore(rule.getScore());
        item.setCorrectAnswer(String.join(",", full.answerKey().optionLabels()));
        item.setReferenceAnswer(full.analysis());
        generated.add(item);
      }
    }
    Collections.shuffle(generated);
    for (int index = 0; index < generated.size(); index++)
      generated.get(index).setOrderNo(index + 1);
    return generated;
  }

  private List<QuestionDO> candidates(long examId, String type) {
    List<Long> bankIds =
        examBanks
            .selectList(
                new LambdaQueryWrapper<MockExamBankDO>().eq(MockExamBankDO::getExamId, examId))
            .stream()
            .map(MockExamBankDO::getQuestionBankId)
            .toList();
    return candidateQuestions(bankIds, type);
  }

  private int candidateCount(List<Long> bankIds, String type) {
    if (bankIds == null || bankIds.isEmpty()) return 0;
    return candidateQuestions(bankIds, type).size();
  }

  private List<QuestionDO> candidateQuestions(List<Long> bankIds, String type) {
    return questionMapper.selectList(
        new LambdaQueryWrapper<QuestionDO>()
            .in(QuestionDO::getQuestionBankId, bankIds)
            .eq(QuestionDO::getQuestionType, type)
            .eq(QuestionDO::getStatus, "ACTIVE"));
  }

  private void score(MockExamAttemptDO attempt) {
    BigDecimal sum = BigDecimal.ZERO;
    for (MockExamAttemptQuestionDO item : attemptQuestionList(attempt.getId())) {
      BigDecimal awarded =
          "SUBJECTIVE".equals(item.getQuestionType())
              ? scoreSubjective(
                  item.getSubjectiveAnswer(), item.getReferenceAnswer(), item.getScore())
              : item.getCorrectAnswer().equals(item.getSelectedAnswer())
                      && !item.getCorrectAnswer().isBlank()
                  ? item.getScore()
                  : BigDecimal.ZERO;
      attemptQuestions.update(
          new LambdaUpdateWrapper<MockExamAttemptQuestionDO>()
              .eq(MockExamAttemptQuestionDO::getId, item.getId())
              .set(MockExamAttemptQuestionDO::getAwardedScore, awarded));
      sum = sum.add(awarded);
    }
    MockExamDO exam = requireExam(attempt.getExamId());
    attempts.update(
        new LambdaUpdateWrapper<MockExamAttemptDO>()
            .eq(MockExamAttemptDO::getId, attempt.getId())
            .set(MockExamAttemptDO::getStatus, "SUBMITTED")
            .set(MockExamAttemptDO::getSubmittedAt, Instant.now())
            .set(MockExamAttemptDO::getScore, sum)
            .set(MockExamAttemptDO::getPassed, sum.compareTo(exam.getPassingScore()) >= 0));
  }

  /** 本地参考答案评分：匹配参考答案中的有效语句，保证没有外部模型时也可自动出分。 */
  private static BigDecimal scoreSubjective(String answer, String reference, BigDecimal max) {
    if (answer == null || answer.isBlank() || reference == null || reference.isBlank())
      return BigDecimal.ZERO;
    List<String> points =
        Arrays.stream(reference.split("[，。；;、\\n]"))
            .map(String::strip)
            .filter(value -> value.length() >= 2)
            .toList();
    if (points.isEmpty()) return BigDecimal.ZERO;
    long hits = points.stream().filter(point -> answer.contains(point)).count();
    return max.multiply(BigDecimal.valueOf(hits))
        .divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
  }

  private Map<String, Object> attemptView(MockExamAttemptDO attempt, boolean includeResult) {
    MockExamDO exam = requireExam(attempt.getExamId());
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", attempt.getId());
    result.put("examId", exam.getId());
    result.put("title", exam.getTitle());
    result.put("durationMinutes", exam.getDurationMinutes());
    result.put("passingScore", exam.getPassingScore());
    result.put("startedAt", attempt.getStartedAt());
    result.put("expiresAt", attempt.getExpiresAt());
    result.put("status", attempt.getStatus());
    if (includeResult) {
      result.put("score", attempt.getScore());
      result.put("passed", attempt.getPassed());
    }
    result.put(
        "questions",
        attemptQuestionList(attempt.getId()).stream()
            .map(item -> attemptQuestionView(item, includeResult))
            .toList());
    return result;
  }

  private Map<String, Object> attemptQuestionView(
      MockExamAttemptQuestionDO item, boolean includeResult) {
    Question question =
        questionRepository
            .findById(new QuestionId(item.getQuestionId()))
            .orElseThrow(() -> new IllegalArgumentException("试题已不存在"));
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("orderNo", item.getOrderNo());
    value.put("questionId", item.getQuestionId());
    value.put("stem", question.stem());
    value.put("questionType", item.getQuestionType());
    value.put("score", item.getScore());
    value.put(
        "options",
        question.options().stream()
            .map(option -> Map.of("label", option.label(), "content", option.content()))
            .toList());
    value.put(
        "selectedAnswers",
        item.getSelectedAnswer().isBlank()
            ? List.of()
            : List.of(item.getSelectedAnswer().split(",")));
    value.put("subjectiveAnswer", item.getSubjectiveAnswer());
    if (includeResult) {
      value.put("awardedScore", item.getAwardedScore());
      value.put("referenceAnswer", item.getReferenceAnswer());
    }
    return value;
  }

  private Map<String, Object> examSummary(MockExamDO exam) {
    List<MockExamRuleDO> examRules = examRules(exam.getId());
    BigDecimal total =
        examRules.stream()
            .map(
                rule ->
                    rule.getScore()
                        .multiply(
                            BigDecimal.valueOf(
                                "ALL".equals(rule.getSelectionMode())
                                    ? candidates(exam.getId(), rule.getQuestionType()).size()
                                    : rule.getQuestionCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("id", exam.getId());
    value.put("title", exam.getTitle());
    value.put("durationMinutes", exam.getDurationMinutes());
    value.put("passingScore", exam.getPassingScore());
    value.put("status", exam.getStatus());
    value.put("totalScore", total);
    value.put(
        "questionBankIds",
        examBanks
            .selectList(
                new LambdaQueryWrapper<MockExamBankDO>()
                    .eq(MockExamBankDO::getExamId, exam.getId()))
            .stream()
            .map(MockExamBankDO::getQuestionBankId)
            .toList());
    value.put(
        "rules",
        examRules.stream()
            .map(
                rule ->
                    Map.of(
                        "questionType",
                        rule.getQuestionType(),
                        "selectionMode",
                        rule.getSelectionMode(),
                        "questionCount",
                        rule.getQuestionCount(),
                        "score",
                        rule.getScore()))
            .toList());
    return value;
  }

  private MockExamDO requireExam(long id) {
    MockExamDO exam = exams.selectById(id);
    if (exam == null) throw new IllegalArgumentException("试卷不存在");
    return exam;
  }

  private MockExamDO requirePublishedExam(long id) {
    MockExamDO exam = requireExam(id);
    if (!"PUBLISHED".equals(exam.getStatus())) throw new IllegalArgumentException("试卷尚未发布");
    return exam;
  }

  private MockExamDO activeConfiguration() {
    MockExamDO exam =
        exams.selectOne(
            new LambdaQueryWrapper<MockExamDO>()
                .eq(MockExamDO::getStatus, "PUBLISHED")
                .orderByDesc(MockExamDO::getPublishedAt)
                .last("limit 1"));
    if (exam == null) throw new IllegalArgumentException("管理员尚未设置模拟考试参数");
    return exam;
  }

  private MockExamAttemptDO requireAttempt(long id) {
    MockExamAttemptDO attempt = attempts.selectById(id);
    if (attempt == null) throw new IllegalArgumentException("考试记录不存在");
    return attempt;
  }

  private MockExamAttemptDO requireOwnedAttempt(JwtAuthenticationToken auth, long id) {
    MockExamAttemptDO attempt = requireAttempt(id);
    if (attempt.getUserId() != PasskeyRegistrationController.currentUser(auth).value())
      throw new IllegalArgumentException("无权访问该考试记录");
    return attempt;
  }

  private MockExamAttemptQuestionDO requireAttemptQuestion(long attemptId, int order) {
    MockExamAttemptQuestionDO item =
        attemptQuestions.selectOne(
            new LambdaQueryWrapper<MockExamAttemptQuestionDO>()
                .eq(MockExamAttemptQuestionDO::getAttemptId, attemptId)
                .eq(MockExamAttemptQuestionDO::getOrderNo, order));
    if (item == null) throw new IllegalArgumentException("试题不存在");
    return item;
  }

  private List<MockExamRuleDO> examRules(long examId) {
    return rules.selectList(
        new LambdaQueryWrapper<MockExamRuleDO>().eq(MockExamRuleDO::getExamId, examId));
  }

  private List<MockExamAttemptQuestionDO> attemptQuestionList(long id) {
    return attemptQuestions.selectList(
        new LambdaQueryWrapper<MockExamAttemptQuestionDO>()
            .eq(MockExamAttemptQuestionDO::getAttemptId, id)
            .orderByAsc(MockExamAttemptQuestionDO::getOrderNo));
  }

  private static boolean isType(String value) {
    return Arrays.stream(QuestionType.values()).anyMatch(type -> type.name().equals(value));
  }

  private static String trim(String value) {
    return value == null ? "" : value.strip();
  }

  private static void requireOpen(MockExamAttemptDO attempt) {
    if (!"IN_PROGRESS".equals(attempt.getStatus())) throw new IllegalArgumentException("该考试已交卷");
    if (!Instant.now().isBefore(attempt.getExpiresAt()))
      throw new IllegalArgumentException("考试已到时，请直接交卷");
  }
}
