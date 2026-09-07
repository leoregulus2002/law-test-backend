# 用户认证与 Passkey 登录设计

## 目标

在现有 Spring Boot 后端新增独立用户模块，支持用户自助注册、账号密码登录和“账号 + Passkey”登录。Android 端通过 Credential Manager 调用 WebAuthn，用户使用指纹、面容或设备 PIN 完成本地验证；后端不接收、不保存任何生物识别数据。

首期同时保护现有 `/api/v1/**` 业务接口，仅放行注册、登录、Token 刷新、OpenAPI 文档、健康检查和 Digital Asset Links。

## 范围

首期包含：

- 用户自助注册；
- 账号密码登录；
- Access Token 与 Refresh Token 的签发、轮换和退出撤销；
- 当前用户信息查询；
- 登录后创建、查看和删除多个 Passkey；
- 输入账号后使用 Passkey 登录；
- WebAuthn challenge 的持久化、过期和单次消费；
- Android Digital Asset Links 响应；
- Spring Security 对现有业务接口的统一保护。

首期不包含：

- 无账号 Passkey 登录；
- 找回密码、修改密码、注销账号和管理员后台；
- 短信、邮箱或第三方登录；
- Access Token 黑名单；
- Redis；
- Android 客户端改造；
- 自动化测试。

## 技术方案

新增与现有 `question` 模块平级的 `user` 模块，继续采用领域、应用、基础设施和 REST 接口分层。Spring Security 负责请求授权、密码校验和 Bearer Token 解析。

引入 Spring Security 的 WebAuthn 支持，使用 `WebAuthnRelyingPartyOperations` 生成注册/认证 options、注册凭证并验证认证断言。后端提供面向原生 Android 的 JSON REST 接口，不启用默认网页、表单登录或基于 HTTP Session 的 WebAuthn 流程。WebAuthn challenge 通过自定义存储端口保存在 PostgreSQL，使认证流程保持无状态并可支持服务重启和多实例部署。

密码使用 BCrypt 编码。Access Token 是短期 JWT；Refresh Token 是高熵不透明随机值，数据库只保存其摘要。

参考资料：

- [Spring Security Passkeys](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passkeys.html)
- [WebAuthn Level 3](https://www.w3.org/TR/webauthn-3/)
- [Android 创建 Passkey](https://developer.android.com/identity/passkeys/create-passkeys)
- [Android Credential Manager 前置要求](https://developer.android.com/identity/credential-manager/prerequisites)

## 数据模型

### `app_user`

保存用户 ID、规范化账号、显示名称、BCrypt 密码摘要、账号状态、连续密码失败次数、锁定截止时间和创建/更新时间。

账号在写入前转为小写并保持唯一，只允许 4–64 位字母、数字、点、下划线或连字符。密码接受 8–72 个字符；72 字符上限与 BCrypt 的输入限制保持一致。显示名称是注册请求中的必填字段，长度为 1–64 个字符。

### `user_passkey`

保存 WebAuthn user handle、credential ID、公钥、签名计数器、AAGUID、备份资格与状态、传输方式、用户填写的凭证名称，以及创建、最近使用时间。一个用户可以拥有多个 Passkey，credential ID 全局唯一。

删除记录表示服务端不再接受该凭证。Android 客户端后续可以使用 Credential Manager Signal API 同步凭证状态，但不属于本次后端范围。

### `auth_ceremony`

保存随机 ceremony ID、32 字节安全随机 challenge、`REGISTER` 或 `AUTHENTICATE` 类型、关联用户、验证所需的序列化 options、创建时间、过期时间和消费时间。

challenge 有效期为 5 分钟。验证开始时通过带条件的原子更新消费记录，条件为“类型匹配、尚未消费且未过期”。即使后续断言验证失败，该 challenge 也不能再次使用。过期记录通过定时清理任务删除；清理不是安全正确性的前提。

### `refresh_session`

保存用户、Refresh Token 摘要、令牌家族 ID、上一个会话 ID、创建、过期、使用和撤销时间。数据库不保存 Refresh Token 明文。

刷新成功后旧会话被标记为已使用，并在同一事务中创建后继会话。已经使用或撤销的 Refresh Token 再次出现时，撤销同一令牌家族中仍有效的所有会话。

## REST 接口

所有请求和响应均使用 JSON，WebAuthn 二进制字段使用无填充 Base64URL。

### 公开接口

- `POST /api/v1/auth/register`：接收 `username`、`password`、`displayName`，创建用户并返回 Token 对。
- `POST /api/v1/auth/password/login`：接收账号密码，返回 Token 对。
- `POST /api/v1/auth/token/refresh`：接收 Refresh Token，轮换并返回新 Token 对。
- `POST /api/v1/auth/logout`：接收 Refresh Token，撤销对应 Refresh Session；接口保持幂等。
- `POST /api/v1/auth/passkeys/authentication/options`：接收账号，创建认证 ceremony，返回 ceremony ID 与 `PublicKeyCredentialRequestOptions`。
- `POST /api/v1/auth/passkeys/authentication/verify`：接收 ceremony ID 和 Android Credential Manager 返回的 assertion，验证后返回 Token 对。
- `GET /.well-known/assetlinks.json`：返回由环境配置生成的 Android 应用关联声明，响应类型固定为 `application/json`。

### 受保护接口

- `GET /api/v1/users/me`：返回当前用户 ID、账号、显示名称、状态和创建时间。
- `POST /api/v1/auth/passkeys/registration/options`：为当前用户创建注册 ceremony，返回 ceremony ID 与 `PublicKeyCredentialCreationOptions`。
- `POST /api/v1/auth/passkeys/registration/verify`：接收 ceremony ID、凭证名称和 Credential Manager 返回的 attestation，验证并保存 Passkey。
- `GET /api/v1/users/me/passkeys`：列出当前用户的 Passkey 元数据，不返回公钥等敏感实现字段。
- `DELETE /api/v1/users/me/passkeys/{credentialId}`：删除当前用户拥有的指定 Passkey，保持幂等。
- 现有 `/api/v1/**` 题库与题目接口。

## 主要流程

### 自助注册与密码登录

注册成功后直接签发 Token 对。重复账号返回冲突错误。密码登录先按规范化账号查找用户并检查状态和锁定时间，再使用恒定成本的 BCrypt 校验。不存在的账号也执行一次固定的 BCrypt 校验，降低账号枚举和时间侧信道风险。

连续 5 次密码失败后锁定账号 15 分钟。登录成功时清零失败计数和锁定时间。Passkey 验证失败不修改密码失败计数。

### 创建 Passkey

用户必须先通过 Bearer Access Token 登录。后端为当前用户生成 user handle 和注册 options，其中 `residentKey=required`、`userVerification=required`、attestation 为 `none`，并将 challenge 保存到 PostgreSQL。Android 使用 Credential Manager 创建凭证并提交 attestation。后端原子消费 ceremony，严格校验 RP ID、Origin、challenge、用户句柄和凭证数据后保存公钥记录。

### Passkey 登录

客户端先提交账号。后端对存在且可用的账号生成带有该用户 credential descriptors 的 request options；对于不存在或不可用的账号，返回结构相同但无法成功验证的 options，以降低账号枚举风险。Android 使用 Credential Manager 让用户选择凭证并生成签名 assertion。

后端原子消费 ceremony，再校验 RP ID、Origin、challenge、credential ID、user handle、签名和签名计数器。成功后更新凭证的计数器及最近使用时间，并签发 Token 对。

## Token 与授权

Access Token 有效期为 15 分钟，包含用户 ID、账号、角色、签发时间、过期时间和 JWT ID，不包含密码、凭证或个人敏感数据。签名密钥至少 256 位，通过环境变量注入；生产环境缺失或过短时应用拒绝启动。

Refresh Token 有效期为 30 天，只在注册、登录和刷新响应中返回。刷新采用轮换和重用检测。退出登录撤销当前 Refresh Session，但已经签发的 Access Token 最多可继续使用 15 分钟。

Spring Security 使用无状态 SecurityContext。除明确列入白名单的端点外，所有 `/api/v1/**` 请求必须携带 `Authorization: Bearer <access-token>`。当前首期只有 `USER` 角色，为后续管理员权限预留角色结构。

## WebAuthn 环境配置

配置项包括 RP ID、RP 显示名称、允许的 Web Origin、允许的 Android APK Origin、ceremony 超时时间、Android 包名与一个或多个签名证书 SHA-256 指纹。

开发环境使用显式开发占位值，并允许通过环境变量覆盖。占位值不能自动回退到生产环境。

生产环境：

- RP ID：`fakao.yanzongkeji.cn`；
- Web Origin：`https://fakao.yanzongkeji.cn`；
- Android Origin：一个或多个 `android:apk-key-hash:<Base64URL 编码的签名证书 SHA-256>`；
- Asset Links 地址：`https://fakao.yanzongkeji.cn/.well-known/assetlinks.json`。

服务端严格使用 Origin 白名单，拒绝未知 Web Origin、未知 Android 应用签名以及 RP ID 不匹配。`assetlinks.json` 必须返回 HTTP 200、无重定向且 Content-Type 为 `application/json`。

## 异常与响应

认证相关错误统一返回：

```json
{
  "code": "AUTHENTICATION_FAILED",
  "message": "账号或凭证无效",
  "timestamp": "2026-09-07T12:00:00Z"
}
```

状态码约定：

- `400 Bad Request`：字段校验失败、WebAuthn 数据格式错误；
- `401 Unauthorized`：密码、Passkey、Refresh Token 或 Access Token 无效；
- `403 Forbidden`：账号锁定或禁用；响应不暴露详细锁定策略；
- `409 Conflict`：注册账号或 credential ID 冲突；
- `410 Gone`：ceremony 已过期或已消费；
- `500 Internal Server Error`：未预期错误，响应不包含密码摘要、Token、challenge、公钥或断言原文。

密码登录和 Passkey 登录对“账号不存在”“密码错误”“凭证无效”使用相同的 `AUTHENTICATION_FAILED` 响应。日志记录内部错误类别和请求关联 ID，但不记录认证秘密。

## 数据一致性与并发

注册账号依赖数据库唯一约束处理并发重复请求。ceremony 消费、Refresh Token 轮换及重用检测使用事务和条件更新。Passkey credential ID 使用全局唯一约束；删除操作同时校验凭证归属。

签名计数器更新遵循 WebAuthn 库的验证结果。计数器异常时拒绝认证并记录安全事件；不自动删除凭证。

## 验证方式

按用户要求，本次不新增或运行自动化测试。实现完成后仅执行 Maven 编译验证，并人工检查：

- Flyway 迁移文件语法和迁移顺序；
- Spring Bean 与 SecurityFilterChain 的编译装配；
- DTO、MyBatis 映射和 WebAuthn 类型转换是否完整；
- OpenAPI 是否包含新增端点；
- `application.yaml` 是否包含开发占位配置且生产配置均可由环境变量覆盖。

编译验证不能替代真实 Android Credential Manager 与生产域名、证书和 Digital Asset Links 的端到端联调；该联调在 Android 客户端接入与部署阶段完成。
