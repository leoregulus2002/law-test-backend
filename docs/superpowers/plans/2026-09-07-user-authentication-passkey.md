# User Authentication and Passkey Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add self-service users, password and account-scoped Passkey login, rotating refresh sessions, and JWT protection for the existing backend API.

**Architecture:** Add a `user` bounded context beside `question`, using application ports and MyBatis Plus adapters for users, WebAuthn credentials, ceremonies, and refresh sessions. Use Spring Security as a stateless JWT resource server and Spring Security WebAuthn's `WebAuthnRelyingPartyOperations` from REST application services instead of its session-oriented browser filters.

**Tech Stack:** Java 26, Spring Boot 4.1.1, Spring Security, `spring-security-webauthn`, Spring Security OAuth2 JOSE/resource server, MyBatis Plus, PostgreSQL, Flyway, Springdoc OpenAPI.

**Spec:** `docs/superpowers/specs/2026-09-07-user-authentication-passkey-design.md`

## Global Constraints

- Production WebAuthn RP ID is exactly `fakao.yanzongkeji.cn` and its Web Origin is exactly `https://fakao.yanzongkeji.cn`.
- Accept Android origins only as configured `android:apk-key-hash:<Base64URL SHA-256>` values; generate Digital Asset Links from the configured package and certificate fingerprints.
- Access Tokens expire after 15 minutes; Refresh Tokens expire after 30 days and rotate on every refresh.
- WebAuthn ceremonies use 32 random bytes, expire after 5 minutes, and are consumed atomically before credential verification.
- Passkey creation sets `residentKey=required`, `userVerification=required`, and attestation `none`.
- Passwords accept 8–72 characters and use BCrypt; accounts are lowercase 4–64 character identifiers matching `[a-z0-9._-]+`.
- Lock password login for 15 minutes after 5 consecutive failures.
- No HTTP Session, form login, plaintext refresh-token storage, biometric data, or Redis.
- The user explicitly requested no automated tests and no test execution. Each task uses compilation and static inspection only.
- Preserve existing question-module behavior except for requiring a Bearer token on `/api/v1/**`.

---

### Task 1: Dependencies, configuration, and database schema

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/resources/db/migration/V1.1.0__create_user_authentication_schema.sql`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/configuration/AuthProperties.java`

**Interfaces:**
- Produces: `AuthProperties` with `jwt`, `webauthn`, and `android` nested configuration records.
- Produces: PostgreSQL tables `app_user`, `user_passkey`, `auth_ceremony`, and `refresh_session` consumed by all persistence adapters.

- [ ] **Step 1: Add managed Spring Security dependencies**

Add these dependencies without explicit versions so Spring Boot controls compatibility:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-webauthn</artifactId>
</dependency>
```

- [ ] **Step 2: Add development-safe authentication configuration**

Bind the following values in `application.yaml`. Keep the development secret long enough for HS256 but require a separate production profile check in Task 4.

```yaml
law-test:
  auth:
    jwt:
      issuer: law-test-backend
      secret: ${JWT_SECRET:dev-only-change-this-32-byte-secret}
      access-token-ttl: PT15M
      refresh-token-ttl: P30D
    webauthn:
      rp-id: ${WEBAUTHN_RP_ID:localhost}
      rp-name: ${WEBAUTHN_RP_NAME:Law Test Development}
      allowed-origins: ${WEBAUTHN_ALLOWED_ORIGINS:http://localhost:8080}
      ceremony-ttl: PT5M
    android:
      package-name: ${ANDROID_PACKAGE_NAME:cn.yanzongkeji.lawtest.android}
      certificate-sha256: ${ANDROID_CERTIFICATE_SHA256:DEV_CERTIFICATE_SHA256}
```

Implement `@ConfigurationProperties(prefix = "law-test.auth")` with `Duration` and `List<String>` fields and reject blank RP IDs, origins, or package names.

- [ ] **Step 3: Create the Flyway migration**

Create all four tables, foreign keys, unique constraints, and lookup indexes. Use `bigint generated always as identity` for users, `uuid` for ceremonies and refresh sessions, `bytea` for WebAuthn binary fields, `jsonb` for serialized options/transport sets, and `timestamp with time zone` for all time values.

The migration must include these concurrency constraints:

```sql
constraint app_user_username_unique unique (username),
constraint user_passkey_credential_id_unique unique (credential_id),
constraint user_passkey_user_label_unique unique (user_id, label),
constraint refresh_session_token_hash_unique unique (token_hash)
```

Add indexes on `user_passkey(user_id)`, `auth_ceremony(expires_at)`, `refresh_session(user_id)`, and `refresh_session(token_family_id)`.

- [ ] **Step 4: Compile and inspect the effective dependency API**

Run: `mvn -DskipTests compile`

Expected: build succeeds after downloading the managed security modules. Before later WebAuthn code, inspect the resolved Spring Security version and compile against that version's public API; do not use deprecated WebAuthn servlet filters.

- [ ] **Step 5: Commit the foundation**

```bash
git add pom.xml src/main/resources/application.yaml src/main/resources/db/migration/V1.1.0__create_user_authentication_schema.sql src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/configuration/AuthProperties.java
git commit -m "feat: add authentication schema and configuration"
```

### Task 2: User domain and persistence

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/model/UserId.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/model/UserStatus.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/model/UserAccount.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/port/UserRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/dataobject/AppUserDO.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/mapper/AppUserMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/repository/MyBatisPlusUserRepository.java`

**Interfaces:**
- Produces: `UserRepository.save(UserAccount)`, `findById(UserId)`, `findByUsername(String)`, `recordPasswordFailure(UserId, Instant)`, and `clearPasswordFailures(UserId)`.
- Produces: `UserAccount.isPasswordLoginAllowed(Instant)` and normalized account validation.

- [ ] **Step 1: Define the user aggregate**

Use a focused aggregate with reconstitution separate from registration:

```java
public final class UserAccount {
    public static UserAccount register(String username, String displayName, String passwordHash, Instant now);
    public static UserAccount reconstitute(UserId id, String username, String displayName,
            String passwordHash, UserStatus status, int failedLoginAttempts,
            Instant lockedUntil, Instant createdAt, Instant updatedAt);
    public boolean isPasswordLoginAllowed(Instant now);
}
```

Normalize usernames with `Locale.ROOT`, enforce `[a-z0-9._-]{4,64}`, require a 1–64 character trimmed display name, and never expose `passwordHash` through REST responses.

- [ ] **Step 2: Implement MyBatis persistence**

Map every database column explicitly where names differ. Use SQL in `AppUserMapper` for atomic failure tracking:

```sql
update app_user
set failed_login_attempts = failed_login_attempts + 1,
    locked_until = case when failed_login_attempts + 1 >= 5
        then #{now} + interval '15 minutes' else locked_until end,
    updated_at = #{now}
where id = #{userId}
```

Successful password or Passkey authentication clears `failed_login_attempts` and `locked_until`.

- [ ] **Step 3: Compile the user persistence slice**

Run: `mvn -DskipTests compile`

Expected: all domain types, mapper SQL annotations, and repository conversions compile without warnings introduced by this task.

- [ ] **Step 4: Commit user persistence**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user/domain src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence
git commit -m "feat: add user account persistence"
```

### Task 3: Refresh-session rotation and token issuance

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/model/RefreshSession.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/port/RefreshSessionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/port/AccessTokenIssuer.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/dataobject/RefreshSessionDO.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/mapper/RefreshSessionMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/repository/MyBatisPlusRefreshSessionRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/security/JwtAccessTokenIssuer.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/auth/TokenPair.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/auth/TokenSessionService.java`

**Interfaces:**
- Produces: `TokenSessionService.issue(UserAccount)`, `refresh(String)`, and `logout(String)`.
- Produces: `TokenPair(String accessToken, String refreshToken, long expiresIn)`.
- Consumes: `UserRepository.findById(UserId)` and `AuthProperties.Jwt`.

- [ ] **Step 1: Implement secure token primitives**

Generate Refresh Tokens from 32 random bytes with `SecureRandom`, encode without Base64URL padding, and store `SHA-256(token)` only. `JwtAccessTokenIssuer.issue(UserAccount, Instant)` must sign HS256 JWTs containing `sub`, `username`, `roles=["USER"]`, `iss`, `iat`, `exp`, and `jti`.

- [ ] **Step 2: Implement refresh rotation transaction**

Annotate `refresh` with `@Transactional`. Lock the session selected by token hash using `select ... for update`. Reject expired/revoked tokens. If `used_at` is already set, revoke every active record sharing its `token_family_id` and throw a generic invalid-token exception. Otherwise mark the current session used and insert a successor with the same family ID.

- [ ] **Step 3: Implement idempotent logout**

Hash the supplied token and set `revoked_at` only when an active matching row exists. Return no indication whether the token existed.

- [ ] **Step 4: Compile token services**

Run: `mvn -DskipTests compile`

Expected: JWT encoder, repository locking query, and transactional service compile.

- [ ] **Step 5: Commit token services**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user/domain src/main/java/cn/yanzongkeji/lawtest/user/application/auth src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/security src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence
git commit -m "feat: add rotating authentication tokens"
```

### Task 4: Registration, password login, and stateless API security

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/auth/AuthUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/auth/AuthService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/exception/AuthenticationFailedException.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/exception/AccountConflictException.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/exception/AccountLockedException.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/RegisterRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasswordLoginRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/RefreshTokenRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/TokenPairResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/AuthController.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/security/SecurityConfiguration.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/configuration/UserModuleConfiguration.java`

**Interfaces:**
- Produces: `AuthUseCase.register`, `passwordLogin`, `refresh`, and `logout` used by `AuthController`.
- Produces: a stateless `SecurityFilterChain` and JWT decoder matching `JwtAccessTokenIssuer`.
- Consumes: `UserRepository`, `PasswordEncoder`, and `TokenSessionService`.

- [ ] **Step 1: Implement self-service registration**

Define records and service methods with exact signatures:

```java
public interface AuthUseCase {
    TokenPair register(String username, String password, String displayName);
    TokenPair passwordLogin(String username, String password);
    TokenPair refresh(String refreshToken);
    void logout(String refreshToken);
}
```

Validate password length before BCrypt encoding. Convert database uniqueness failures for `app_user.username` to `AccountConflictException`. Registration returns `TokenSessionService.issue(savedUser)`.

- [ ] **Step 2: Implement password-login timing and lock behavior**

Keep one precomputed BCrypt hash for nonexistent accounts and always call `passwordEncoder.matches`. For existing active accounts, enforce `lockedUntil`, record every mismatch atomically, clear failures on success, then issue tokens. Return the same `AuthenticationFailedException` for a missing account or password mismatch.

- [ ] **Step 3: Configure stateless Spring Security**

Disable CSRF, form login, HTTP Basic, request cache, and server sessions. Permit the documented public auth endpoints, `/.well-known/assetlinks.json`, `/v3/api-docs/**`, Swagger/Knife4j resources, and `/actuator/health` if Actuator is present. Require authentication for every other `/api/v1/**` route. Convert JWT `roles` claims to `ROLE_USER` authorities and return JSON 401/403 responses.

- [ ] **Step 4: Reject unsafe production configuration**

Add a startup validator active under the `prod` profile that requires a secret of at least 32 UTF-8 bytes, RP ID `fakao.yanzongkeji.cn`, HTTPS Web Origin, and at least one non-placeholder Android certificate fingerprint. The default/development profile continues to use the explicit development values from Task 1.

- [ ] **Step 5: Compile authentication endpoints and security**

Run: `mvn -DskipTests compile`

Expected: all controllers, JWT beans, security matchers, and application services compile.

- [ ] **Step 6: Commit password authentication**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user
git commit -m "feat: add password authentication and API security"
```

### Task 5: PostgreSQL WebAuthn repositories and one-time ceremonies

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/model/AuthCeremony.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/domain/port/AuthCeremonyRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/dataobject/AuthCeremonyDO.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/dataobject/UserPasskeyDO.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/mapper/AuthCeremonyMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/mapper/UserPasskeyMapper.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/persistence/repository/PostgresAuthCeremonyRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/webauthn/PostgresPublicKeyCredentialUserEntityRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/webauthn/PostgresUserCredentialRepository.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/webauthn/WebAuthnConfiguration.java`

**Interfaces:**
- Implements: Spring's `PublicKeyCredentialUserEntityRepository.findById(Bytes)`, `findByUsername(String)`, `save(PublicKeyCredentialUserEntity)`, and `delete(Bytes)`.
- Implements: Spring's `UserCredentialRepository.findByCredentialId(Bytes)`, `findByUserId(Bytes)`, `save(CredentialRecord)`, and `delete(Bytes)`.
- Produces: `AuthCeremonyRepository.create(...)` and `consume(UUID, CeremonyType, Instant)`.
- Produces: configured `WebAuthnRelyingPartyOperations`.

- [ ] **Step 1: Implement lossless WebAuthn mappings**

Persist and reconstruct every `CredentialRecord` field required by the resolved Spring Security API: credential type and ID, COSE public key, signature count, UV initialization, transports, backup eligibility/state, user-entity ID, attestation object/client data when present, label, created time, and last-used time. Store enum sets as JSON arrays and use Spring Security `Bytes` only at the adapter boundary.

- [ ] **Step 2: Implement atomic ceremony consumption**

`consume` must issue one conditional update and return the pre-update ceremony payload only when this predicate holds:

```sql
where id = #{id}
  and ceremony_type = #{type}
  and consumed_at is null
  and expires_at > #{now}
returning *
```

If no row returns, throw `CeremonyUnavailableException` mapped to HTTP 410. Never validate a credential using an unconsumed or expired record.

- [ ] **Step 3: Configure relying-party operations**

Construct `PublicKeyCredentialRpEntity` from `AuthProperties`, create `Webauthn4JRelyingPartyOperations(userEntities, credentials, rpEntity, allowedOrigins)`, and customize creation/request options to require user verification and resident credentials with a five-minute timeout. Include both HTTPS and `android:apk-key-hash:` origins from configuration.

- [ ] **Step 4: Compile WebAuthn adapters**

Run: `mvn -DskipTests compile`

Expected: custom repositories implement the exact non-deprecated public interfaces of the resolved Spring Security version.

- [ ] **Step 5: Commit WebAuthn persistence**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user/domain src/main/java/cn/yanzongkeji/lawtest/user/infrastructure
git commit -m "feat: persist WebAuthn credentials and ceremonies"
```

### Task 6: Passkey registration and management

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyRegistrationUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyRegistrationService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyManagementUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyManagementService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyOptions.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyRegistrationVerifyRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/PasskeyOptionsResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/PasskeyResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/PasskeyRegistrationController.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/UserController.java`

**Interfaces:**
- Produces: `beginRegistration(UserId)` returning ceremony ID plus `PublicKeyCredentialCreationOptions`.
- Produces: `finishRegistration(UserId, UUID, String, PublicKeyCredential<AuthenticatorAttestationResponse>)`.
- Produces: list/delete operations scoped to the authenticated user.
- Consumes: `WebAuthnRelyingPartyOperations`, `AuthCeremonyRepository`, and credential repositories from Task 5.

- [ ] **Step 1: Generate registration options for the JWT user**

Resolve the user ID from JWT `sub`, load the account, and create or reuse one stable random 32-byte WebAuthn user handle for that account. Pass username and display name to `ImmutablePublicKeyCredentialCreationOptionsRequest`, persist the exact returned options as the ceremony payload, and return `{ceremonyId, publicKey}`.

- [ ] **Step 2: Verify and store registration responses**

Consume the ceremony before calling `registerCredential`. Confirm the ceremony user equals the authenticated JWT user. Build `ImmutableRelyingPartyRegistrationRequest` from the submitted attestation, stored creation options, and trimmed 1–64 character label. Translate duplicate credential IDs or duplicate per-user labels to HTTP 409.

- [ ] **Step 3: Add credential listing and deletion**

List only credential ID, label, transports, backup flags, created time, and last-used time. Decode the path credential ID as Base64URL and delete only when its user entity belongs to the authenticated user. A missing/already deleted credential returns 204 to keep deletion idempotent.

- [ ] **Step 4: Compile registration and management endpoints**

Run: `mvn -DskipTests compile`

Expected: registration JSON types serialize, ownership checks compile, and no Session API is referenced.

- [ ] **Step 5: Commit Passkey registration**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user/application/passkey src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest
git commit -m "feat: add Passkey registration and management"
```

### Task 7: Account-scoped Passkey authentication

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyAuthenticationUseCase.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/application/passkey/PasskeyAuthenticationService.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyAuthenticationOptionsRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/request/PasskeyAuthenticationVerifyRequest.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/PasskeyAuthenticationController.java`

**Interfaces:**
- Produces: `beginAuthentication(String username)` returning ceremony ID plus `PublicKeyCredentialRequestOptions`.
- Produces: `finishAuthentication(UUID, PublicKeyCredential<AuthenticatorAssertionResponse>)` returning `TokenPair`.
- Consumes: `TokenSessionService.issue(UserAccount)` and all Task 5 WebAuthn ports.

- [ ] **Step 1: Generate account-scoped authentication options**

Normalize the supplied username. For active existing accounts, build request options whose `allowCredentials` contains only that user's credentials. For missing, locked, disabled, or credential-less accounts, generate and persist a dummy ceremony with a random nonmatching credential descriptor and return the same response shape and status.

- [ ] **Step 2: Verify assertions and issue tokens**

Consume the ceremony first. Reject dummy ceremonies with `AuthenticationFailedException`. Build `RelyingPartyAuthenticationRequest` from the assertion and stored request options, invoke `WebAuthnRelyingPartyOperations.authenticate`, and verify that the returned user entity maps to the ceremony's expected user. On success, clear password failure state, allow the credential adapter to persist the new signature count/last-used values, and issue a Token pair.

- [ ] **Step 3: Normalize all authentication failures**

Map malformed assertions, unknown credentials, signature failures, RP/origin/challenge mismatches, and user-handle mismatches to HTTP 401 with `AUTHENTICATION_FAILED`. Log only the internal category, ceremony ID, and request correlation ID; never log assertion bodies.

- [ ] **Step 4: Compile the complete Passkey login flow**

Run: `mvn -DskipTests compile`

Expected: authentication options and assertion DTOs compile against Android-compatible WebAuthn JSON structures.

- [ ] **Step 5: Commit Passkey authentication**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user/application/passkey src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest
git commit -m "feat: add account Passkey authentication"
```

### Task 8: Error contract, current user, and Digital Asset Links

**Files:**
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/AuthErrorResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/response/CurrentUserResponse.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/UserAuthenticationExceptionHandler.java`
- Create: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/AssetLinksController.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/user/interfaces/rest/controller/UserController.java`
- Modify: `src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/controller/WordQuestionExceptionHandler.java`

**Interfaces:**
- Produces: consistent `{code,message,timestamp}` auth error JSON.
- Produces: `GET /api/v1/users/me` and `GET /.well-known/assetlinks.json`.
- Prevents: generic question advice from converting user-module exceptions into misleading Word parsing errors.

- [ ] **Step 1: Add ordered exception handling**

Handle validation as 400, authentication/token failures as 401, locked/disabled accounts as 403, unique conflicts as 409, unavailable ceremonies as 410, and unexpected errors as sanitized 500 responses. Give the user advice higher precedence and narrow the existing question advice so it no longer catches every `IllegalArgumentException` globally.

- [ ] **Step 2: Add the current-user endpoint**

Read JWT `sub`, load the user, and return only `id`, `username`, `displayName`, `status`, and `createdAt`.

- [ ] **Step 3: Generate Digital Asset Links**

Return a JSON array containing one Android target whose `sha256_cert_fingerprints` array contains every configured signing fingerprint:

```json
[{"relation":["delegate_permission/common.handle_all_urls","delegate_permission/common.get_login_creds"],"target":{"namespace":"android_app","package_name":"cn.yanzongkeji.lawtest.android","sha256_cert_fingerprints":["AA:BB:CC"]}}]
```

Set `Content-Type: application/json`; do not redirect this route. Preserve colon-delimited SHA-256 values in Asset Links while separately Base64URL-encoding them for the WebAuthn Android Origin allowlist.

- [ ] **Step 4: Compile all production sources**

Run: `mvn -DskipTests compile`

Expected: full application compile succeeds with no newly introduced compiler error.

- [ ] **Step 5: Commit REST completion**

```bash
git add src/main/java/cn/yanzongkeji/lawtest/user src/main/java/cn/yanzongkeji/lawtest/question/interfaces/rest/controller/WordQuestionExceptionHandler.java
git commit -m "feat: complete user authentication API"
```

### Task 9: Final static verification and documentation

**Files:**
- Modify: `src/main/resources/application.yaml` only if compile-time configuration inspection finds a mismatch
- Modify: `docs/superpowers/specs/2026-09-07-user-authentication-passkey-design.md` only if the resolved Spring Security public API requires a documented implementation adjustment

**Interfaces:**
- Verifies: dependency compatibility, production source compilation, migration ordering, route authorization, and absence of secrets.

- [ ] **Step 1: Run the user-requested verification command**

Run: `mvn -DskipTests clean compile`

Expected: `BUILD SUCCESS`. Do not run `test`, `verify`, or integration tests.

- [ ] **Step 2: Inspect the migration and route coverage**

Run:

```bash
rg -n "create table (app_user|user_passkey|auth_ceremony|refresh_session)|unique|foreign key|expires_at|consumed_at" src/main/resources/db/migration/V1.1.0__create_user_authentication_schema.sql
rg -n "permitAll|authenticated|STATELESS|csrf|formLogin|httpBasic" src/main/java/cn/yanzongkeji/lawtest/user/infrastructure/security/SecurityConfiguration.java
```

Expected: all four tables and required constraints are present; only documented public routes are permitted and all remaining `/api/v1/**` routes are authenticated.

- [ ] **Step 3: Scan for leaked secrets and unfinished markers**

Run:

```bash
rg -n "passwordHash|refreshToken|JWT_SECRET|DEV_CERTIFICATE" src/main/java src/main/resources
rg -n "T[B]D|T[O]DO|FIX[M]E|changeme" src/main/java src/main/resources
```

Expected: secret field names occur only in internal models/configuration and are not logged or serialized; no unfinished markers exist; the development JWT fallback is clearly development-only and production validation rejects it.

- [ ] **Step 4: Review the complete diff**

Run: `git status --short && git diff --stat HEAD~8..HEAD`

Expected: only authentication implementation, the targeted exception-advice narrowing, migration/configuration, and approved documentation changes appear.

- [ ] **Step 5: Commit verification-only corrections if needed**

When Step 1–4 require a correction, compile again and commit only those reviewed corrections:

```bash
git add src/main/java src/main/resources docs/superpowers/specs/2026-09-07-user-authentication-passkey-design.md
git commit -m "fix: align authentication integration"
```
