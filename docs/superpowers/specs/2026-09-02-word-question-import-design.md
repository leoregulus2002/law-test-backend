# Word 题目导入持久化设计

## 目标

将现有 `POST /api/v1/questions/parse-word` 从“只解析”改为“解析成功后导入题库”。一次上传对应一个题库；题库、题目、选项和正确答案在同一事务内写入 PostgreSQL。提供题库查询，以及题目的查询、新增、完整更新和删除接口，并支持删除题库。

## 约束

- 保持题目领域对象为充血模型，领域层不依赖 Spring、MyBatis-Plus 或 HTTP。
- 不使用物理外键；关系完整性由聚合规则、唯一约束、索引和仓储事务维护。
- Word 内容的 SHA-256 是导入幂等键。相同二进制内容再次上传不得重复写入。
- 不新增或执行自动化测试；实现后只进行编译及 Flyway/Compose 配置检查。
- 新的 Flyway 文件使用 `V主.次.修订__说明.sql` 格式。

## 领域模型

新增 `QuestionBank` 与 `Question` 两个独立聚合根，以及 `QuestionBankCode`、`QuestionBankId`、`QuestionId` 值对象。

- `QuestionBankCode` 保存 64 位小写十六进制 SHA-256。
- `QuestionBank` 仅管理题库的 ID、导入文件名、显示名称和幂等编码。
- `Question` 管理自身 ID、所属题库 ID、题号、题干、选项、答案和解析；创建及更新时校验选项不能为空且标识不重复，答案必须引用题目已有选项。
- `QuestionType` 由正确答案数量推导：一个正确选项为 `SINGLE_CHOICE`，两个或以上为 `MULTIPLE_CHOICE`。
- `Question` 的更新行为由聚合方法完成，接口 DTO、DO 和 Mapper 不进入领域层。
- 同一题库内题号不可重复：数据库唯一约束负责最终并发保护，应用层将冲突转译为 HTTP 409。

题库显示名称从上传文件名去除 `.docx` 后获得，文件名仅保存末段，避免把客户端路径写入数据库。

## 应用流程

`ImportWordQuestionsUseCase` 接收文件名和流：

1. 校验上传文件名和扩展名。
2. 以 `DigestInputStream` 将 Word 交给现有解析器，在不额外缓存文件的前提下计算 SHA-256。
3. 使用摘要创建 `QuestionBank`。
4. 调用 `QuestionBankRepository.createIfAbsent`。
5. 仅当题库新建成功时，调用 `QuestionRepository.saveAll` 保存题目聚合。
6. 返回题库数据库 ID、题库编码、题目数与本次是否实际导入。

解析失败时不会调用仓储。仓储返回“已存在”时，不会写入题目、选项或答案。

## 持久化适配器

`MyBatisPlusQuestionBankRepository` 和 `MyBatisPlusQuestionRepository` 分别实现领域仓储端口，使用 MyBatis-Plus Mapper 和数据对象（DO）。DO 只存在于基础设施层，不能泄漏至领域或接口层。

先以 `question_bank.code` 执行 `INSERT ... ON CONFLICT DO NOTHING RETURNING id`，借助现有唯一约束处理并发重复上传。获得新题库 ID 后，依次写入 `question`、`question_option` 和 `question_answer`；这些写入运行在一个应用服务事务中。`question_bank_id` 与 `question_id` 的普通索引保留，以支撑后续按题库或题目批量查询。

题目、选项和答案的写入顺序由仓储维护；更新题目时先删除旧答案和选项，再写入新选项和答案；删除题目时先删除答案和选项；删除题库时先按上述顺序删除所有题目数据，再删除题库。由于没有物理外键，禁止绕过仓储直接修改这四张表。

列表和详情查询使用独立的只读查询端口投影为响应 DTO，不为读取而加载不需要修改的完整聚合。

## HTTP 接口

保留现有路径和上传字段：`POST /api/v1/questions/parse-word`，`multipart/form-data` 的 `file`。

成功响应保留解析后的题目列表，并新增：

- `questionBankId`：已保存题库的数据库 ID。
- `questionBankCode`：内容 SHA-256 幂等编码。
- `imported`：`true` 表示本次新建；`false` 表示相同内容此前已导入。

格式错误和文件读取错误维持 HTTP 400。数据库写入失败由统一异常处理转换为 HTTP 500，且不返回内部 SQL 细节。

### 题库接口

- `GET /api/v1/question-banks?page=0&size=20`：题库分页查询；`size` 最大 100。
- `GET /api/v1/question-banks/{questionBankId}`：题库详情。
- `DELETE /api/v1/question-banks/{questionBankId}`：删除题库及其中全部题目数据，返回 HTTP 204。

题库不提供更新接口。

### 题目接口

- `GET /api/v1/question-banks/{questionBankId}/questions?page=0&size=20`：题库内题目分页查询。
- `GET /api/v1/questions/{questionId}`：题目详情，含选项、答案和解析。
- `POST /api/v1/question-banks/{questionBankId}/questions`：新增一道题目，返回 HTTP 201。
- `PUT /api/v1/questions/{questionId}`：完整替换一道题目的题号、题干、选项、答案和解析，返回 HTTP 200。
- `DELETE /api/v1/questions/{questionId}`：删除一道题及其选项、答案，返回 HTTP 204。

题库或题目不存在返回 HTTP 404；题号冲突返回 HTTP 409；题目内容不满足领域规则返回 HTTP 400。

## 数据库迁移

不需要增加物理关联约束。现有 `question_bank.code varchar(64)` 可以直接保存 SHA-256；其唯一约束用于幂等性。已有的 `V1.0.1` 迁移会去除 V1 中遗留的物理外键。

## 非目标

- 不保存原始 Word 二进制文件。
- 不保存重复上传的文件名或重复的解析结果。
