# 考试模式最小闭环设计规格

## 背景

当前平台已经完成练习提交、RabbitMQ 判题消息、独立 Judge Worker、Docker C 语言沙箱和判题结果回写。数据库初始迁移已经包含 `exams`、`exam_problems` 和 `submissions.exam_id`，共享枚举 `JudgeMode` 也已经包含 `EXAM`。

本规格只覆盖任务 10 的第一阶段：让学生能进入考试、提交考试题目、查看自己的考试成绩；管理员考试 CRUD 和前端界面留给后续任务补齐。

## 方案选择

### 方案 A：一次性完成完整考试后台

一次性实现管理员创建、编辑、绑定题目、查看所有成绩，学生考试提交和成绩汇总也同时完成。

优点是接口一次成型；缺点是本轮改动面大，测试组合多，容易把学生提交闭环和后台管理细节耦合在一起。

### 方案 B：先完成学生考试闭环，管理员数据用现有 schema 和测试数据驱动

先实现开放考试查询、考试详情、考试内提交、我的考试成绩。管理员创建和编辑接口只在设计上预留边界，不在本轮实现。

优点是能最快打通真实考试判题路径，改动集中在 `exam` 包和 `SubmissionService`，风险可控；缺点是需要通过数据库种子或后续管理接口来创建考试。

### 方案 C：把考试提交复用为练习提交，只加 `exam_id`

在练习提交流程上加一个可选 `examId`，尽量少建考试服务。

优点是代码最少；缺点是考试时间、题目归属、成绩规则会散落在提交服务里，后续做管理员结果页和考试规则时边界不清。

推荐方案是 B。考试是独立业务边界，但本轮先做可用闭环，不提前实现完整后台。

## 目标

1. 学生能查询当前可见的考试列表。
2. 学生能查看某场考试详情和题目列表。
3. 学生只能在考试开放时间内提交该考试绑定的题目。
4. 考试提交保存到 `submissions`，`exam_id` 为当前考试 ID，判题消息 `mode` 为 `EXAM`。
5. 学生能查看自己的考试成绩汇总。
6. 成绩规则第一版固定为每题取该学生在该考试内最高分。

## 非目标

1. 本轮不实现管理员考试 CRUD。
2. 本轮不实现班级、报名、准考证、考试密码。
3. 本轮不实现考试结束后隐藏源码、封榜、排名。
4. 本轮不实现按最后一次提交计分、部分分测试点计分或人工评分。
5. 本轮不修改 Worker 判题算法；Worker 只需要透传并处理 `JudgeMode.EXAM` 消息。

## 数据模型

复用现有表结构：

- `exams`：考试基础信息，使用 `enabled` 控制是否对学生可见。
- `exam_problems`：考试题目绑定表，`score` 是考试内题目分值，允许与题库原始分值不同。
- `submissions`：考试提交通过 `exam_id` 关联考试。

本轮不新增迁移。为后续查询效率，可以在后续迁移中补充 `idx_submissions_exam_user_problem`，但这不是第一阶段必须条件。

## 服务边界

新增 `clearn-api/src/main/java/com/clearn/api/exam/` 包：

- `Exam`：映射 `exams` 表。
- `ExamProblem`：映射考试题目绑定结果。
- `ExamMapper`：提供考试、考试题目、考试提交成绩查询。
- `ExamService`：承载考试时间校验、题目归属校验、考试提交创建和成绩汇总。
- `ExamController`：学生端考试接口。
- `dto`：请求和响应模型。

`SubmissionService` 保留练习提交入口，并暴露一个面向考试服务的内部方法来创建考试提交，或由 `ExamService` 复用同等校验逻辑后直接插入提交。推荐把公共的源码校验和提交入库能力留在 `SubmissionService`，考试规则留在 `ExamService`，避免提交服务理解完整考试业务。

## 学生接口

### `GET /api/exams`

返回当前可见考试列表。

第一版可见规则：

- `enabled = true`
- 默认返回当前时间之前已经开始且尚未结束的考试

响应字段：

- `id`
- `title`
- `description`
- `startTime`
- `endTime`

### `GET /api/exams/{id}`

返回考试详情和题目列表。

访问规则：

- 考试必须存在且 `enabled = true`
- 第一版允许在开放时间内查看详情
- 返回题目信息不包含隐藏测试用例

题目字段：

- `problemId`
- `title`
- `difficulty`
- `score`
- `sortOrder`

### `POST /api/exams/{examId}/problems/{problemId}/submissions`

创建考试提交。

请求体复用练习提交格式：

```json
{
  "sourceCode": "int main(void) { return 0; }"
}
```

校验顺序：

1. `examId`、`problemId`、当前用户 ID 不为空。
2. 源码非空且不超过 `clearn.submission.max-source-bytes`。
3. 考试存在且 `enabled = true`。
4. 当前时间在 `[start_time, end_time]` 内。
5. 题目属于该考试。
6. 题目仍然是启用状态。

成功后：

- 插入 `submissions`，状态为 `PENDING`，`exam_id = examId`。
- 事务提交后发布 `JudgeTaskMessage`。
- 消息字段 `mode = EXAM`，`examId = examId`。

失败响应沿用现有控制器风格：

- 参数或业务规则错误返回 `400`
- 考试或题目不存在返回 `404`

### `GET /api/exams/{id}/my-result`

返回当前学生在该考试中的成绩汇总。

汇总规则：

- 每道考试题取该学生在该考试内最高 `score`。
- 没有提交的题得 0。
- 总分为各题得分之和。
- `status` 不直接参与统计，第一版依赖 Worker 回写的 `score`。当前判题规则下 AC 得题目分，非 AC 得 0。

响应字段：

- `examId`
- `totalScore`
- `maxScore`
- `problems`
- 每题包含 `problemId`、`title`、`score`、`maxScore`、`bestSubmissionId`、`bestStatus`。

## 时间处理

服务层统一使用 UTC 进行比较：

- 数据库 `TIMESTAMP` 读出为 `LocalDateTime` 后按 UTC 解释。
- API 响应使用 `OffsetDateTime`，offset 为 UTC。

测试中使用相对当前时间的数据，避免固定日期导致测试随时间失效。

## 判题消息

考试提交的消息示例：

```json
{
  "submissionId": 10001,
  "problemId": 3001,
  "language": "C",
  "mode": "EXAM",
  "examId": 5001,
  "createdAt": "2026-06-17T03:00:00Z"
}
```

Worker 第一阶段不需要按考试模式改变判题逻辑。考试模式只影响主系统提交归属和成绩查询。

## 测试策略

服务测试覆盖：

1. 考试未开始时拒绝提交。
2. 考试已结束时拒绝提交。
3. 题目不属于考试时拒绝提交。
4. 考试内提交会保存 `exam_id` 并发布 `EXAM` 判题消息。
5. 判题消息仍在事务提交后发布。
6. 成绩汇总按每题最高分计算，未提交题为 0。

控制器测试覆盖：

1. 学生能查询开放考试。
2. 学生能查看考试详情。
3. 学生能创建考试提交。
4. 学生只能查看自己的考试结果。
5. 未登录请求由现有安全配置拦截。

## 实现顺序

1. 新增 `ExamMapper` 和基本实体，只读查询先落地。
2. 新增 `ExamService` 的考试时间和题目归属校验测试。
3. 实现考试提交创建，并复用提交发布后的 RabbitMQ 事务语义。
4. 实现考试成绩汇总查询。
5. 新增 `ExamController` 暴露学生接口。
6. 运行 `clearn-api` 测试，再运行全量 Maven 测试。

## 验收标准

1. `JudgeMode.EXAM` 消息可以从 API 产生。
2. `submissions.exam_id` 正确记录考试归属。
3. 考试窗口外无法提交。
4. 非考试题无法作为考试提交。
5. 我的考试结果按每题最高分汇总。
6. 现有练习提交、Worker 判题和回写测试不回归。
