# Question 持久化层职责重构设计

## 目标

在保留 MyBatis-Plus、现有领域模型及 REST 接口行为的前提下，将 Question 聚合生命周期、管理端列表查询、练习场景查询和数据库访问职责分离，并消除分页加载完整题目时的 N+1 查询。

## 架构边界

### QuestionRepository

`question/domain/port/QuestionRepository` 仅表达 Question 聚合生命周期：

- `findById(QuestionId)`：恢复一个完整 Question 聚合。
- `save(Question)`：新增或更新一个完整 Question 聚合。
- `saveAll(QuestionBankId, Collection<Question>)`：批量保存导入得到的聚合。
- `deleteById(QuestionId)`：删除聚合及其子表数据。
- `deleteByBankId(QuestionBankId)`：删除题库下全部聚合及其子表数据。

分页、统计、游标和随机抽题方法全部移出该领域端口。

### QuestionManagementQuery

`question/application/query/QuestionManagementQuery` 服务题目管理列表用例：

- 按题库分页加载完整 Question 聚合。
- 统计指定题库的题目数量。

它的基础设施实现为 `MyBatisPlusQuestionManagementQuery`。分页接口仍返回完整 Question，以保持现有控制器响应不变。

### QuestionPracticeQuery

`question/application/query/QuestionPracticeQuery` 服务练习/答题用例：

- 统计全部或指定题库范围内的题目数量。
- 按 ID 游标查询题目 ID。
- 在全部或指定题库范围内随机查询一个题目 ID。

它的基础设施实现为 `MyBatisPlusQuestionPracticeQuery`。这些查询只读取所需 ID 或数量，不恢复聚合。

### Mapper

- `QuestionMapper` 负责 question 表的聚合写入辅助和单表访问。
- `QuestionOptionMapper` 负责 question_option 表的单表查询、写入和删除。
- `QuestionAnswerMapper` 负责 question_answer 表的单表查询、写入和删除。
- `QuestionQueryMapper` 负责分页、统计、游标和 PostgreSQL 随机查询等查询侧 SQL。

Application 和 Domain 层不直接依赖任何 Mapper。

## 聚合转换

新增 `QuestionPersistenceConverter`，统一完成：

- Question 聚合到 QuestionDO 的转换。
- QuestionDO、QuestionOptionDO、QuestionAnswerDO 到 Question 聚合的恢复。

聚合恢复必须调用 `Question.reconstitute(...)`，不绕过现有领域校验规则，也不引入 MapStruct。

## 数据流

### 单聚合读取

1. Repository 使用 QuestionMapper 查询 question。
2. 使用对应子表 Mapper 分别查询 options 和 answers。
3. Converter 恢复完整 Question 聚合。

单聚合读取固定执行三次查询。

### 分页读取

1. QuestionQueryMapper 分页查询 QuestionDO。
2. 收集页面内全部 questionId。
3. QuestionQueryMapper 使用一次 `IN` 查询全部 QuestionOptionDO。
4. QuestionQueryMapper 使用一次 `IN` 查询全部 QuestionAnswerDO。
5. Java 内存中按 questionId 分组，再由 Converter 逐个恢复聚合。

非空页面固定为三次查询，替代原来的 `1 + 2N` 次查询。空页面仅执行一次主表查询。

### 保存与删除

Repository 负责跨 question、question_option、question_answer 三表的拆分、写入顺序及子表清理。SQL 条件封装在对应 Mapper 方法中，Repository 不再直接构造 LambdaQueryWrapper。

## 事务

现有写用例已经由 `QuestionManagementService` 和 `ImportWordQuestionsService` 的 `@Transactional` 方法包裹。继续以 Application Service 为事务边界，使聚合主表和子表修改处于同一事务；不把事务拆到单个 Mapper 方法。

## 兼容性与约束

- 不修改数据库表结构，不新增 Flyway 脚本。
- 不改变领域模型及其业务规则。
- 不改变控制器路径、请求参数、响应 DTO 或结果排序语义。
- 保留 PostgreSQL `ORDER BY random()` 的随机抽题行为，并将 SQL 下沉到 QuestionQueryMapper。
- 保留 MyBatis-Plus BaseMapper。
- 更新 QuestionQueryService、Spring 组合根及现有测试桩的依赖，使其适配新查询端口。
- 不新增或运行测试；完成后只执行 Maven 编译验证。
- 为新增和修改的重要方法添加中文注释，并遵循现有 Java 代码风格。

## 非目标

不引入 Event Sourcing、消息队列、Saga、CQRS 框架、通用 Repository 基类、额外 Adapter 层或数据库结构调整。
