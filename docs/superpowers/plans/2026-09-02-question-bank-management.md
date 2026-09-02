# 题库导入与题目管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Word 解析结果幂等导入 PostgreSQL，并提供题库查询、题目查询/新增/更新/删除和题库删除 REST 接口。

**Architecture:** `QuestionBank` 与 `Question` 是独立聚合根，通过领域 ID 逻辑关联。应用服务负责事务和用例编排，领域层负责不变量，MyBatis-Plus Mapper/DO/仓储适配器负责持久化与只读投影；数据库不使用物理外键。

**Tech Stack:** Java 26、Spring Boot 4.1.1、PostgreSQL 18、Flyway、MyBatis-Plus 3.5.17、Apache POI、Lombok、Knife4j。

**Spec:** `docs/superpowers/specs/2026-09-02-word-question-import-design.md`

## Global Constraints

- 领域层不得依赖 Spring、MyBatis-Plus、HTTP 或 DO。
- 所有新增 Flyway 文件采用 `V主.次.修订__说明.sql`；不得改写已执行迁移。
- 不使用物理外键；删除和更新的关联维护由仓储在事务中完成。
- 同一 Word 二进制内容以 SHA-256 幂等，重复导入不写入重复题目。
- 不新增或执行自动化测试；只运行 `mvn -DskipTests compile`、Flyway 启动检查与 Compose 配置检查。

---

### Task 1: Reshape domain aggregates and ports

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionBank.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionBankCode.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionBankId.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionId.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/Question.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionBankRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionBankQueryPort.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionQueryPort.java`

**Interfaces:**
- `QuestionBank.create(String sourceFileName, QuestionBankCode code)` returns a validated bank without persistence dependencies.
- `Question.create(QuestionBankId bankId, QuestionNumber number, String stem, List<QuestionOption> options, AnswerKey answerKey, String analysis)` builds a new aggregate; `revise(...)` replaces all editable state only after domain validation.
- `QuestionBankRepository.createIfAbsent(QuestionBank)` returns the generated `QuestionBankId` and an `imported` flag.
- `QuestionRepository.saveAll(QuestionBankId, Collection<Question>)`, `findById(QuestionId)`, `save(Question)`, `deleteById(QuestionId)`, and `deleteByQuestionBankId(QuestionBankId)` express command-side persistence.
- Query ports return application-neutral read projections and paged results, not DOs or REST records.

- [ ] Add positive-only ID value objects and a SHA-256 value object that accepts exactly 64 lowercase hexadecimal characters.
- [ ] Add `QuestionBank` creation rules: filename must yield a nonblank display name and source filename must not contain client path segments.
- [ ] Extend `Question` with bank identity, optional persisted identity and `revise` behavior; preserve existing option and answer consistency validation.
- [ ] Replace the unused `QuestionRepository.saveAll(Collection<Question>)` signature with command methods needed by import and management.
- [ ] Define command and read port result records without referencing Spring, MyBatis-Plus or HTTP types.

### Task 2: Add application commands, queries and transactional import

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/ImportWordQuestionsUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/ImportWordQuestionsService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionManagementUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionManagementService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionBankQueryUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionBankQueryService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionNotFoundException.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionBankNotFoundException.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/QuestionNumberConflictException.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/application/ParseWordQuestionsService.java`

**Interfaces:**
- `ImportWordQuestionsUseCase.importWord(String fileName, InputStream content)` returns `WordImportResult(questionBankId, questionBankCode, imported, questions)`.
- `QuestionManagementUseCase` exposes create, replace and delete commands using application command records, never REST request records.
- `QuestionBankQueryUseCase` exposes page and detail reads for banks and questions, plus bank deletion.

- [ ] Extract common Word file validation from the existing parse service so parsing and importing share the same extension/content rules.
- [ ] Implement import with `DigestInputStream` and `MessageDigest.getInstance("SHA-256")`; parse before any repository call, then create the bank and save parsed questions only when the bank is new.
- [ ] Mark the import, create, replace and delete use-case methods transactional so every multi-table write commits or rolls back together.
- [ ] Convert domain validation failures into application exceptions that distinguish malformed input (400), missing resources (404) and duplicate question number (409).
- [ ] Implement query use cases with page/size validation: zero-based page, default size 20, maximum size 100.

### Task 3: Implement MyBatis-Plus persistence and read adapters

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/dataobject/QuestionBankDataObject.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/dataobject/QuestionDataObject.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/dataobject/QuestionOptionDataObject.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/dataobject/QuestionAnswerDataObject.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionBankMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionOptionMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionAnswerMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/repository/MyBatisPlusQuestionBankRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/repository/MyBatisPlusQuestionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/query/MyBatisPlusQuestionBankQueryAdapter.java`

**Interfaces:**
- `QuestionBankMapper.insertIfAbsent` uses `INSERT INTO question_bank (...) VALUES (...) ON CONFLICT (code) DO NOTHING RETURNING id`.
- `QuestionMapper` uses generated identity IDs; option and answer writes use those generated question IDs.
- Query adapter executes paged bank and question queries, and batched `IN` reads for options and answers; it must not issue one query per question.

- [ ] Define Lombok-backed DOs with only table/column mappings, generated IDs and no domain behavior.
- [ ] Make every Mapper explicit with `@Mapper`; this removes the current empty-mapper scan warning once persistence is present.
- [ ] Implement idempotent bank insertion and return an existing ID without writing child rows when the code already exists.
- [ ] Implement question insert order as question, options, answers; use `question_type = MULTIPLE_CHOICE` and let PostgreSQL provide timestamp and JSON defaults.
- [ ] Implement question replacement and deletion in the required order: delete answers, delete options, update/delete question, then insert new options and answers as applicable.
- [ ] Implement bank deletion by selecting its question IDs and deleting answers/options/questions/bank in one transaction; return whether a bank was found.
- [ ] Implement page count and data queries with stable ordering: banks by `id desc`, questions by `sequence_no asc, id asc`.

### Task 4: Wire Spring configuration and HTTP contracts

**Files:**
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/configuration/QuestionModuleConfiguration.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/configuration/OpenApiConfiguration.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordQuestionController.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordParseResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionBankController.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionController.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionBankResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionDetailResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionUpsertRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionOptionRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/PageResponse.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordQuestionExceptionHandler.java`

**Interfaces:**
- Import response adds `questionBankId`, `questionBankCode` and `imported` while retaining parsed questions.
- `POST /api/v1/question-banks/{questionBankId}/questions` returns 201 and the created question.
- `PUT /api/v1/questions/{questionId}` returns 200 and the replacement question.
- Both delete endpoints return 204 on success; resource lookup failures return 404.

- [ ] Register parser, repositories, query adapter and application use cases as Spring beans without importing Spring into the domain package.
- [ ] Change `POST /api/v1/questions/parse-word` to call the import use case and document idempotent behavior in Knife4j/OpenAPI.
- [ ] Add bank list/detail/delete endpoints with validated page parameters and explicit response schemas.
- [ ] Add question list/detail/create/replace/delete endpoints; map request data to application commands rather than passing REST records to the domain.
- [ ] Extend exception handling: input/domain exceptions to 400, not-found exceptions to 404, sequence-number conflict to 409, and unexpected persistence failures to a generic 500 `ProblemDetail`.

### Task 5: Verify configuration without tests

**Files:**
- Verify: `src/main/resources/db/migration/V1.0.1__remove_question_bank_foreign_keys.sql`
- Verify: `compose.yaml`
- Verify: project compilation

- [ ] Run `docker compose config --quiet` to validate Compose syntax.
- [ ] Restart the already-running Spring Boot application once so Flyway applies `V1.0.1`; confirm the log records the migration without calling endpoints.
- [ ] Run `mvn -DskipTests compile` to compile production code without executing any tests.
- [ ] Run `git diff --check` and inspect Flyway history to confirm no migration checksum was rewritten.
