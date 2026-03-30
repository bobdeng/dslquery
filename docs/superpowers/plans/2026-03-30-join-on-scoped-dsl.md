# Join On Scoped DSL Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional scoped DSL conditions for `join on` while preserving existing annotation-driven join behavior.

**Architecture:** Introduce a small join-on annotation and a runtime query API that both flow into `JoinField`. Render extra `on` predicates with a dedicated scoped SQL builder so existing `where` parsing and SQL generation remain stable.

**Tech Stack:** Java, JUnit 5, Gradle

---

## Chunk 1: API and failing coverage

### Task 1: Add design-facing tests for annotation and runtime join-on

**Files:**
- Modify: `src/test/java/cn/beagile/dslquery/DeepJoinTest.java`
- Modify: `src/test/java/cn/beagile/dslquery/DSLQueryTest.java`

- [ ] **Step 1: Write the failing tests**
- [ ] **Step 2: Run the focused test command and confirm the new tests fail for the expected reason**
- [ ] **Step 3: Keep existing snapshots unchanged**

### Task 2: Add scoped resolution coverage

**Files:**
- Create: `src/test/java/cn/beagile/dslquery/JoinOnSQLBuilderTest.java`

- [ ] **Step 1: Write failing tests for `self`, `parent`, `root`, and unknown-field handling**
- [ ] **Step 2: Run the focused test command and confirm the failures are correct**

## Chunk 2: Minimal implementation

### Task 3: Add the new annotation and runtime API

**Files:**
- Create: `src/main/java/cn/beagile/dslquery/JoinOn.java`
- Modify: `src/main/java/cn/beagile/dslquery/DSLQuery.java`

- [ ] **Step 1: Add `@JoinOn` with runtime retention on fields**
- [ ] **Step 2: Add `DSLQuery.joinOn(path, dsl)` and storage for runtime join-on conditions**
- [ ] **Step 3: Run focused tests**

### Task 4: Render join-on DSL in `JoinField`

**Files:**
- Modify: `src/main/java/cn/beagile/dslquery/ColumnFields.java`
- Modify: `src/main/java/cn/beagile/dslquery/JoinField.java`
- Create: `src/main/java/cn/beagile/dslquery/JoinOnSQLBuilder.java`
- Modify: `src/main/java/cn/beagile/dslquery/SQLBuilder.java`
- Modify: `src/main/java/cn/beagile/dslquery/SingleExpression.java`

- [ ] **Step 1: Thread merged join-on strings into each `JoinField`**
- [ ] **Step 2: Build scoped field lookup for `self`, `parent`, and `root`**
- [ ] **Step 3: Support `@fieldRef` values only when the builder opts in**
- [ ] **Step 4: Run focused tests and make them pass**

## Chunk 3: Verification

### Task 5: Regression verification

**Files:**
- Modify: `src/test/java/cn/beagile/dslquery/ColumnFieldsTest.java`

- [ ] **Step 1: Add a regression test proving joins are unchanged without join-on**
- [ ] **Step 2: Run focused join-related test suites**
- [ ] **Step 3: Run a broader repository test command if practical**
