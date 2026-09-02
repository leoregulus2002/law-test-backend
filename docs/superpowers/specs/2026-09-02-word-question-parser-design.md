# Word 题目解析模块设计

## 目标

提供一个上传 `.docx` 的 HTTP 接口，将题目 Word 中的表格解析为题目 JSON。当前不持久化数据，但必须为未来数据库持久化保留领域仓储端口。

## 输入格式

支持题目表格。每个题目表格由两列五行组成，左列标签分别为 `题号`、`题目`、`选项`、`答案`、`解析`；右列为相应内容。选项单元格由 `A.`、`B.` 等开头的段落组成，答案为无分隔或以分隔符分开的选项标识。

解析器只处理包含题目字段标签的表格。发现这类表格后，五个字段缺失、题号/题目/答案为空、选项标识重复或答案引用不存在的选项，都会拒绝整次请求，并返回表格序号及原因。非题目表格被忽略。

## 严格 DDD 边界

`question` 是唯一的有界上下文，按职责划分为以下层次：

- `domain`：`Question` 聚合、`QuestionOption` 值对象、`QuestionNumber` 值对象、`AnswerKey` 值对象，以及 `WordQuestionParser` 与 `QuestionRepository` 端口。领域对象以私有构造器与命名工厂创建，主动校验不变量；不存在只有字段的贫血实体。
- `application`：`ParseWordQuestionsUseCase` 处理上传命令、调用领域解析端口并返回领域题目。此层不理解 Apache POI、HTTP 或数据库。
- `interfaces.rest`：Spring MVC 控制器和请求/响应 DTO；负责 multipart 适配、调用用例、把领域对象转换为 JSON，不将框架类型带入领域层。
- `infrastructure.word`：Apache POI 的 `.docx` 适配器，实现 `WordQuestionParser`。它负责定位表格与单元格、读取段落、转换为领域对象。
- `infrastructure.configuration`：组合根，装配解析器及 OpenAPI 元数据。

后续添加数据库时，应用服务可依赖 `QuestionRepository` 端口并在基础设施层增加 JPA/MyBatis 适配器；不修改控制器、Apache POI 解析器或领域模型规则。

## 富领域模型

`Question` 维护题号、题干、选项、答案与解析，并在创建时确保：题号和题干非空、选项至少一个且标识唯一、答案非空且全部存在于选项中。`QuestionOption` 确保标识为单个大写英文字母且内容非空；`AnswerKey` 确保答案标识无重复。领域对象通过查询方法暴露语义数据，而非公共 setter。

## HTTP 合约

`POST /api/v1/questions/parse-word`

- 请求：`multipart/form-data`，字段名 `file`。
- 成功：`200 OK`，响应包含 `fileName`、`questionCount` 与 `questions`。每道题含 `number`、`stem`、`options`、`answers`、`analysis`。
- 失败：`400 Bad Request`，响应体为 Spring 的 problem detail，明确说明不支持的文件、空文件或表格结构问题。
- 限制：仅 `.docx`，文件大小由 Spring multipart 配置限制为 10MB。

## 文档与依赖

Apache POI 5.5.1 用于 Word 表格读取。项目为 Spring Boot 4.1.1，采用 Springdoc OpenAPI 3.0.3 生成 OpenAPI 数据，并添加 Knife4j 4.5.0 的独立 OpenAPI3 UI WebJar；避免使用与旧 Springdoc 绑定的 Knife4j Starter。文档入口为 `/doc.html`，OpenAPI JSON 为 `/v3/api-docs`。

## 非目标

- 不保存到数据库、不创建表、不实现仓储适配器。
- 不支持 `.doc`、PDF、图片 OCR 或任意自由文本排版。
- 按用户要求，不新增或执行自动化测试；完成时仅执行跳过测试的 Maven 编译检查。
