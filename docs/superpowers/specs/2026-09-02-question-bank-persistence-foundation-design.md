# 题库持久化基础设施设计

## 目标

为现有题目解析上下文提供 PostgreSQL、Flyway 与 MyBatis-Plus 的本地开发基础设施，但不改变当前上传接口的“仅解析、不保存”行为。

## 运行环境

`compose.yaml` 启动 PostgreSQL 18，使用 `law-test-postgres-data` 具名卷保存数据。端口、数据库、用户名与密码可以由 `.env` 或 shell 环境变量覆盖；`.env.example` 仅提供本地开发模板，真实 `.env` 被 Git 忽略。

## 应用集成

Spring Boot 使用 PostgreSQL DataSource；Flyway 自动执行 `classpath:db/migration`；MyBatis-Plus 只提供基础设施能力与下划线转驼峰映射。本阶段不创建 Mapper、DO 或 `QuestionRepository` 实现，避免引入违反当前“暂不持久化”范围的写操作。

## 迁移版本规范

新的 Flyway 迁移统一使用 `V主版本.次版本.修订号__说明.sql`，例如 `V1.0.1__remove_question_bank_foreign_keys.sql`。已部署的 `V1` 保持不可变，以确保 Flyway 历史校验通过；从下一条迁移开始采用三段式版本号。

## 初始数据模型

- `question_bank`：题库来源与唯一编码。
- `question`：题库内题号、题干、解析、题型和原始解析 JSON。
- `question_option`：按题目保存选项标识、内容与展示顺序。
- `question_answer`：将正确答案约束为所属题目的有效选项。

表使用顺序 `bigint identity` 主键、非空与唯一约束。表之间仅使用逻辑关联，不使用物理外键：仓储在同一事务内按 `Question` 聚合完成写入、删除和答案-选项一致性校验。`question_bank_id` 与 `question_id` 均保留普通索引，原始 JSON 暂不建 GIN 索引，直到出现以 JSON 条件检索的真实需求。

`question`、`question_option`、`question_answer` 是一个题目聚合的持久化展开，而不是三个独立的领域聚合。选项与答案被拆分是为支持可变数量选项和多选答案；读取列表只访问 `question`，读取详情时仓储应按题目 ID 批量读取选项和答案，避免 N+1 查询。

## 非目标

- 不修改 Word 解析接口为写入数据库。
- 不实现 MyBatis-Plus Mapper、持久化对象或仓储适配器。
- 不创建知识点、试卷、用户答题或全文检索表；这些由后续明确需求驱动。
