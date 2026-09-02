# 题库持久化基础设施 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Docker Compose 启动 PostgreSQL，并为未来题库持久化接入 Flyway 和 MyBatis-Plus。

**Architecture:** Docker Compose 管理本地 PostgreSQL 生命周期；Flyway 管理不可变数据库迁移；MyBatis-Plus 仅作为未来基础设施仓储适配器的持久化工具。当前解析用例不写库。

**Tech Stack:** PostgreSQL 18、Docker Compose、Flyway、MyBatis-Plus 3.5.17、Spring Boot 4.1.1。

**Migration Versioning:** 新迁移使用 `V主版本.次版本.修订号__说明.sql`；已执行的迁移文件保持不可变。

**Spec:** `docs/superpowers/specs/2026-09-02-question-bank-persistence-foundation-design.md`

## Global Constraints

- 本地凭据只能来自可覆盖的环境变量，`.env` 不纳入版本控制。
- 数据库数据必须存入具名卷。
- 不改变现有 Word 解析接口的持久化行为。
- 不新增或执行自动化测试。

---

### Task 1: Add database runtime configuration

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yaml`

- [ ] Add MyBatis-Plus Boot 4 starter, Flyway PostgreSQL support and runtime PostgreSQL driver.
- [ ] Configure datasource from `POSTGRES_*` variables, Flyway migration location, and underscore-to-camel-case mapping.

### Task 2: Define local PostgreSQL lifecycle

**Files:**
- Create: `compose.yaml`
- Create: `.env.example`
- Modify: `.gitignore`

- [ ] Add PostgreSQL 18 service, health check, named volume and overrideable environment values.
- [ ] Ignore the user-owned `.env` file and document its local default values.

### Task 3: Establish versioned question-bank schema

**Files:**
- Create: `src/main/resources/db/migration/V1__create_question_bank_schema.sql`

- [ ] Create bank, question, option and answer tables with identity primary keys, integrity constraints and logical-relation indexes; do not use physical foreign keys.

### Task 4: Verify local infrastructure

**Files:**
- Verify: `compose.yaml`, migration and application configuration

- [ ] Run `docker compose config`, `docker compose up -d`, and `docker compose ps` to verify PostgreSQL becomes healthy.
- [ ] Run `mvn -DskipTests compile` without executing tests.
