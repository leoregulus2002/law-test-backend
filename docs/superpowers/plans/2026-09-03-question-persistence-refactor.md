# Question Persistence Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Question 聚合持久化、管理列表查询和练习查询分离，并把分页完整聚合加载从 N+1 查询降为固定三次查询。

**Architecture:** `QuestionRepository` 只管理聚合生命周期；`QuestionManagementQuery` 和 `QuestionPracticeQuery` 分别表达管理及练习场景的读取需求。基础设施使用 MyBatis-Plus Mapper 和显式 SQL，实现类通过 `QuestionPersistenceConverter` 恢复领域聚合。

**Tech Stack:** Java 26、Spring Boot 4、MyBatis-Plus 3.5、PostgreSQL、Maven

**Spec:** `docs/superpowers/specs/2026-09-03-question-persistence-refactor-design.md`

## Global Constraints

- 不修改数据库表结构，不新增 Flyway 脚本。
- 不改变领域模型业务规则、REST 接口及返回结果。
- 保留 MyBatis-Plus，不引入 MapStruct 或通用 Repository 抽象。
- 新增和修改的重要方法添加中文注释。
- 按用户要求不新增或运行测试，只执行 Maven 编译验证。

---

### Task 1: 收紧领域 Repository 并建立查询端口

**Files:**
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/domain/port/QuestionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/query/QuestionManagementQuery.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/application/query/QuestionPracticeQuery.java`

**Interfaces:**
- Produces: `QuestionManagementQuery.findPageByBankId(QuestionBankId, int, int): List<Question>`
- Produces: `QuestionManagementQuery.countByBankId(QuestionBankId): long`
- Produces: `QuestionPracticeQuery.countByBankIds(List<QuestionBankId>): long`
- Produces: `QuestionPracticeQuery.findIdsAfter(List<QuestionBankId>, Long, int): List<QuestionId>`
- Produces: `QuestionPracticeQuery.findRandomId(List<QuestionBankId>): Optional<QuestionId>`

- [ ] 从 `QuestionRepository` 删除分页、单题库统计、多题库统计、游标和随机查询方法及不再需要的 `List` import。
- [ ] 新增 `QuestionManagementQuery`，只声明管理列表所需的分页和统计语义。
- [ ] 新增 `QuestionPracticeQuery`，只声明练习场景所需的统计、游标和随机语义。
- [ ] 为两个查询端口添加中文接口说明及重要方法注释。

### Task 2: 下沉数据库访问并集中聚合转换

**Files:**
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionMapper.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionOptionMapper.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionAnswerMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/mapper/QuestionQueryMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/converter/QuestionPersistenceConverter.java`

**Interfaces:**
- Produces: 单表 Mapper 的按题目查询、按题目批量查询、按题目删除及按题库删除辅助方法。
- Produces: `QuestionQueryMapper` 的分页、统计、游标及随机 ID 查询方法。
- Produces: `QuestionPersistenceConverter.toQuestionDO(Question, QuestionBankId): QuestionDO`
- Produces: `QuestionPersistenceConverter.toDomain(QuestionDO, List<QuestionOptionDO>, List<QuestionAnswerDO>): Question`

- [ ] 在 `QuestionMapper` 中增加按题库查询主键和按题库删除方法，保留 `updateContent`。
- [ ] 在 Option、Answer Mapper 中增加按单个/多个 questionId 查询及删除方法；查询结果保持选项显示顺序和答案标签顺序。
- [ ] 新增 `QuestionQueryMapper`，使用 MyBatis 动态 SQL 完成分页、统计、游标和 PostgreSQL `ORDER BY random()` 查询。
- [ ] 新增 `QuestionPersistenceConverter`，通过 `Question.reconstitute(...)` 恢复聚合，并集中维护 DO 转换。

### Task 3: 精简 Repository 并实现固定次数分页查询

**Files:**
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/repository/MyBatisPlusQuestionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/query/MyBatisPlusQuestionManagementQuery.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/persistence/query/MyBatisPlusQuestionPracticeQuery.java`

**Interfaces:**
- Consumes: Task 1 的三个端口和 Task 2 的 Mapper、Converter。
- Produces: Question 聚合生命周期实现、管理查询实现和练习查询实现。

- [ ] 重写 `MyBatisPlusQuestionRepository`，移除全部查询侧方法和 LambdaQueryWrapper，只保留聚合保存、单聚合恢复及级联删除协调。
- [ ] 新增 `MyBatisPlusQuestionManagementQuery`：分页查 QuestionDO 后，分别批量查 OptionDO 和 AnswerDO，按 questionId 分组并恢复聚合；空页面不发起子表查询。
- [ ] 新增 `MyBatisPlusQuestionPracticeQuery`：把值对象 ID 转为数据库 Long，并将统计、游标和随机查询委托给 `QuestionQueryMapper`。
- [ ] 确认非空分页严格为一次 question 查询、一次 option 查询和一次 answer 查询。

### Task 4: 迁移应用调用方并编译验证

**Files:**
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/application/query/QuestionQueryService.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/infrastructure/configuration/QuestionModuleConfiguration.java`
- Modify: `src/test/java/cn/yanzongkeji/lawtest/question/application/query/QuestionQueryServiceTest.java`

**Interfaces:**
- Consumes: `QuestionRepository`、`QuestionManagementQuery`、`QuestionPracticeQuery`。
- Produces: 与现有 `QuestionQueryUseCase` 完全相同的外部行为。

- [ ] 将 `QuestionQueryService` 构造依赖拆为 QuestionRepository、QuestionManagementQuery 和 QuestionPracticeQuery，并迁移每个调用点。
- [ ] 更新 `QuestionModuleConfiguration` 组合根，将三个依赖注入查询服务。
- [ ] 更新现有测试桩构造方式和接口实现，使源码继续匹配新的端口；不新增或运行测试。
- [ ] 运行 `mvn -Dmaven.test.skip=true compile`，要求退出码为 0。
- [ ] 运行 `git diff --check` 并检查最终 diff，确认无数据库迁移、接口层和领域模型行为变更。
