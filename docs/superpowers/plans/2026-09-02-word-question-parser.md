# Word 题目解析模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 通过严格 DDD 的充血领域模型解析题目 Word，并以 Knife4j 文档化的 HTTP 接口返回 JSON。

**Architecture:** `question` 有界上下文由领域模型与端口、应用用例、REST 适配器、Apache POI 基础设施适配器组成。领域对象在工厂方法中维护题目、选项和答案的一致性；未来持久化通过仓储端口接入，不改变当前解析流程。

**Tech Stack:** Java 26、Spring Boot 4.1.1、Apache POI 5.5.1、Springdoc OpenAPI 3.0.3、Knife4j UI 4.5.0、Lombok。

**Spec:** `docs/superpowers/specs/2026-09-02-word-question-parser-design.md`

## Global Constraints

- 仅接受 `.docx`，最大 10MB。
- 当前解析结果只返回，不持久化。
- 领域层不得引用 Spring、Apache POI、Servlet 或 OpenAPI 类型。
- `Question` 必须是无 setter 的充血聚合，并在创建时校验题目不变量。
- 不新增或运行测试；验证仅为 `./mvnw -DskipTests compile`。

---

### Task 1: Build and documentation dependencies

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yaml`

**Interfaces:**
- Produces: Apache POI、Springdoc 与 Knife4j UI 的运行时依赖，以及 `/doc.html` 与 `/v3/api-docs` 的配置。

- [ ] **Step 1: Add fixed dependency versions**

Add Maven properties `apache-poi.version=5.5.1`, `springdoc.version=3.0.3`, and `knife4j.version=4.5.0`.

- [ ] **Step 2: Add required libraries**

Add `org.apache.poi:poi-ooxml`, `org.springdoc:springdoc-openapi-starter-webmvc-ui`, and `com.github.xiaoymin:knife4j-openapi3-ui` using those properties.

- [ ] **Step 3: Configure multipart and OpenAPI paths**

Set Spring multipart limits to 10MB and retain the default OpenAPI endpoint `/v3/api-docs`.

### Task 2: Implement rich domain model and ports

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/Question.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionOption.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/QuestionNumber.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/model/AnswerKey.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/WordQuestionParser.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionRepository.java`

**Interfaces:**
- Produces: `Question.create(QuestionNumber, String, List<QuestionOption>, AnswerKey, String)`, `WordQuestionParser.parse(InputStream)`, `QuestionRepository.saveAll(List<Question>)`.

- [ ] **Step 1: Implement value objects**

Use Java records with validating compact constructors. `QuestionOption` requires a single `A`-`Z` identifier and nonblank content. `AnswerKey` requires a nonempty, duplicate-free answer collection. `QuestionNumber` requires a positive integer.

- [ ] **Step 2: Implement the Question aggregate**

Use a private constructor and `create` factory. Reject blank stem, duplicate options, and answer identifiers absent from options. Expose semantic query methods only.

- [ ] **Step 3: Define domain ports**

Define parser and future repository interfaces independent from frameworks and concrete infrastructure.

### Task 3: Implement application use case and Apache POI adapter

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/ParseWordQuestionsUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/ParseWordQuestionsService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/word/ApachePoiWordQuestionParser.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/word/InvalidWordQuestionFormatException.java`

**Interfaces:**
- Consumes: `WordQuestionParser.parse(InputStream)`.
- Produces: `ParseWordQuestionsUseCase.parse(String fileName, InputStream content): List<Question>`.

- [ ] **Step 1: Implement application validation**

Reject blank filenames and non-`.docx` filenames, then delegate the stream untouched to `WordQuestionParser`.

- [ ] **Step 2: Implement table extraction**

Open `XWPFDocument`, locate tables containing question labels, map left-cell labels to right-cell contents, require all five labels, and create each `Question` through the domain factory.

- [ ] **Step 3: Parse options and answers**

Parse option paragraphs using `^([A-Z])[.．、、)]\\s*(.+)$`; concatenate continuation paragraphs to the previous option. Normalize answer strings into individual upper-case identifiers and retain line breaks in analysis.

- [ ] **Step 4: Report actionable invalid format errors**

Convert malformed OpenXML, missing fields, unparseable options, and domain invariant failures into an exception naming the one-based Word table index.

### Task 4: Expose REST API and Knife4j metadata

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordQuestionController.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordParseResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/QuestionOptionResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/configuration/OpenApiConfiguration.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/WordQuestionExceptionHandler.java`

**Interfaces:**
- Consumes: multipart field `file` at `POST /api/v1/questions/parse-word`.
- Produces: `WordParseResponse(fileName, questionCount, questions)` and problem detail errors.

- [ ] **Step 1: Add REST DTO mapping**

Map `Question` to an immutable response that exposes question number, stem, ordered options, answer identifiers and analysis without exposing domain internals.

- [ ] **Step 2: Add multipart controller**

Use `@RequestPart("file") MultipartFile file`; reject empty input and call `ParseWordQuestionsUseCase`. Document consumes/response types with OpenAPI annotations.

- [ ] **Step 3: Add problem-detail exception mapping**

Map `InvalidWordQuestionFormatException` and application validation failures to `400 Bad Request` using `ProblemDetail`.

- [ ] **Step 4: Add OpenAPI information**

Publish an `OpenAPI` bean titled `法考题目解析接口`, with version `v1` and a description that explains the `.docx` table contract.

### Task 5: Compile verification

**Files:**
- Verify: all production source and configuration files above

- [ ] **Step 1: Compile without running tests**

Run: `./mvnw -DskipTests compile`

Expected: Maven reports `BUILD SUCCESS`; no tests run.
