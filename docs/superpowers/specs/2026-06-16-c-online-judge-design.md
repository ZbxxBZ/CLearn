# C 语言在线刷题平台设计规格

## 目标

建设一个面向真实多人在线使用的 C 语言在线刷题平台，支持学生端和管理员端。平台需要提供刷题模式、考试模式、管理员手动维护题库，并通过独立 Judge Worker 在隔离环境中完成 C 代码编译、运行和判题。

第一版重点是可用、安全边界清楚、便于扩展，而不是覆盖所有 Online Judge 高级功能。

## 技术选型

- 主系统：Spring Boot、Spring Security、MyBatis、MySQL
- 消息队列：RabbitMQ
- 判题服务：Java Judge Worker
- 代码执行隔离：Docker 沙箱
- 缓存与限流：Redis
- 前端形态：学生端和管理员端共用同一 Web 应用，按角色显示不同功能

## 系统边界

主系统负责业务数据和任务分发，不直接编译或运行学生提交的代码。

Judge Worker 负责消费判题任务、创建沙箱、编译 C 代码、运行测试用例、比对输出、回写结果。

Docker 沙箱只运行不可信代码。沙箱需要关闭网络、限制 CPU、限制内存、限制进程数、隔离文件系统，并在每次判题后销毁临时环境。

## 角色

### 学生

- 登录平台
- 浏览题目列表
- 查看题目详情
- 在刷题模式提交 C 代码
- 查看每次提交的判题状态和结果
- 进入考试并提交考试题目
- 查看自己考试内的提交和分数

### 管理员

- 登录后台
- 新增、编辑、禁用题目
- 为题目添加公开样例和隐藏测试用例
- 设置题目时间限制、内存限制、分值、难度和标签
- 创建考试，配置考试时间、题目集合和提交规则
- 查看学生提交记录、考试结果和判题异常

## 核心业务流程

### 刷题模式

1. 学生选择题目并提交 C 代码。
2. 主系统保存提交记录，状态为 `PENDING`。
3. 主系统向 RabbitMQ 发送判题任务。
4. Judge Worker 消费任务。
5. Worker 拉取提交源码、题目限制和测试用例。
6. Worker 编译源码。
7. 编译失败时返回 `CE`。
8. 编译成功后，Worker 对每个测试用例启动隔离运行。
9. Worker 汇总判题结果并回写提交记录。
10. 学生端轮询或通过后续 WebSocket 能力查看结果。

### 考试模式

1. 管理员创建考试并绑定题目。
2. 学生在考试开放时间内进入考试。
3. 学生提交代码时，主系统校验考试时间、身份和提交规则。
4. 判题流程与刷题模式一致。
5. 主系统根据考试规则统计每题最高分或最后一次有效提交分数。
6. 管理员查看考试成绩和提交明细。

## 判题状态

- `PENDING`：等待判题
- `JUDGING`：正在判题
- `AC`：答案正确
- `WA`：答案错误
- `CE`：编译错误
- `TLE`：运行超时
- `MLE`：内存超限
- `RE`：运行错误
- `SE`：系统错误

## 判题任务消息

RabbitMQ 消息只放必要索引，不放完整源码和测试用例内容。

```json
{
  "submissionId": 10001,
  "problemId": 3001,
  "language": "C",
  "mode": "PRACTICE",
  "examId": null,
  "createdAt": "2026-06-16T01:55:00+08:00"
}
```

Worker 收到消息后，通过主系统内部接口或直接访问数据库读取判题所需数据。第一版推荐 Worker 使用受限数据库账号读取必要数据，并通过主系统内部 API 回写结果。

## Judge Worker 流程

1. 将提交状态更新为 `JUDGING`。
2. 创建独立工作目录。
3. 写入 `main.c`。
4. 执行 `gcc -O2 -std=c11 main.c -o main`。
5. 编译失败时保存编译错误摘要，结果为 `CE`。
6. 对每个测试用例启动独立 Docker 容器。
7. 容器使用只读基础镜像，挂载只包含可执行文件和输入文件的临时目录。
8. 容器关闭网络并设置 CPU、内存、进程数和运行时间限制。
9. 收集标准输出、标准错误、退出码、耗时和内存使用。
10. 将程序输出与标准输出进行规范化比对。
11. 遇到首个失败用例时可提前结束，记录失败类型。
12. 所有用例通过时结果为 `AC`。
13. 回写判题结果、耗时、内存和错误摘要。
14. 删除临时目录和容器。

## 输出比对规则

第一版采用普通文本比对：

- 忽略末尾多余空白字符
- 保留行内空格差异
- 保留大小写差异
- 不支持 Special Judge

后续可扩展 Special Judge、浮点误差比对和多答案比对。

## 数据模型

### users

- `id`
- `username`
- `password_hash`
- `role`
- `enabled`
- `created_at`

### problems

- `id`
- `title`
- `description`
- `input_description`
- `output_description`
- `difficulty`
- `tags`
- `time_limit_ms`
- `memory_limit_mb`
- `score`
- `enabled`
- `created_at`
- `updated_at`

### test_cases

- `id`
- `problem_id`
- `input_data`
- `expected_output`
- `sample`
- `sort_order`
- `created_at`

### submissions

- `id`
- `user_id`
- `problem_id`
- `exam_id`
- `language`
- `source_code`
- `status`
- `score`
- `time_used_ms`
- `memory_used_kb`
- `error_message`
- `created_at`
- `judged_at`

### exams

- `id`
- `title`
- `description`
- `start_time`
- `end_time`
- `enabled`
- `created_at`

### exam_problems

- `id`
- `exam_id`
- `problem_id`
- `score`
- `sort_order`

## API 范围

### 学生端

- `POST /api/auth/login`
- `GET /api/problems`
- `GET /api/problems/{id}`
- `POST /api/problems/{id}/submissions`
- `GET /api/submissions/{id}`
- `GET /api/submissions/my`
- `GET /api/exams`
- `GET /api/exams/{id}`
- `POST /api/exams/{examId}/problems/{problemId}/submissions`
- `GET /api/exams/{id}/my-result`

### 管理员端

- `POST /api/admin/problems`
- `PUT /api/admin/problems/{id}`
- `POST /api/admin/problems/{id}/test-cases`
- `PUT /api/admin/test-cases/{id}`
- `DELETE /api/admin/test-cases/{id}`
- `POST /api/admin/exams`
- `PUT /api/admin/exams/{id}`
- `POST /api/admin/exams/{id}/problems`
- `GET /api/admin/submissions`
- `GET /api/admin/exams/{id}/results`

### Worker 内部接口

- `POST /api/internal/judge/submissions/{id}/start`
- `POST /api/internal/judge/submissions/{id}/finish`
- `POST /api/internal/judge/submissions/{id}/system-error`

内部接口需要独立鉴权，不使用学生或管理员 token。

## 安全设计

- 用户代码只在 Judge Worker 节点执行。
- Worker 节点与主业务服务分开部署。
- Docker 容器运行时关闭网络。
- 容器设置 CPU、内存、进程数、运行时间限制。
- 每次判题使用独立临时目录。
- 判题结束后删除临时文件。
- Worker 使用低权限系统用户运行。
- Worker 与主系统通信使用内部密钥。
- 管理员接口需要 RBAC 控制。
- 登录密码只保存哈希。
- 提交频率需要限流，防止恶意刷队列。
- 测试用例对学生不可见，公开样例除外。

## 并发设计

- Web 服务只创建任务，不阻塞等待判题完成。
- RabbitMQ 承接提交洪峰。
- Judge Worker 可横向扩展多个实例。
- 每个 Worker 限制本机并发判题数，避免资源耗尽。
- 提交状态使用幂等更新，防止消息重投导致重复回写。
- 判题任务失败时允许重试，超过次数后标记为 `SE`。

## 第一版范围

包含：

- 学生和管理员登录
- 学生刷题提交
- 管理员手动维护题目和测试用例
- 基础考试模式
- RabbitMQ 异步判题
- Java Judge Worker
- Docker 沙箱运行 C 代码
- 基础文本输出比对
- 提交记录和考试成绩查看

不包含：

- 多语言判题
- Special Judge
- 代码查重
- 班级和课程体系
- 排行榜
- 题目批量导入导出
- Worker 自动扩缩容
- WebSocket 实时推送

## 测试策略

- 主系统使用单元测试覆盖权限、题库、提交和考试规则。
- 使用集成测试验证提交后会创建 RabbitMQ 判题消息。
- Worker 使用单元测试覆盖编译失败、输出错误、超时、运行错误和正确结果。
- 使用端到端测试验证学生提交代码后最终得到判题结果。
- 使用安全测试验证容器不能访问网络、不能写入非临时目录、不能无限 fork。

## 实施顺序

1. 建立数据库表结构和基础项目分层。
2. 实现登录和角色权限。
3. 实现题库和测试用例管理。
4. 实现学生题目列表、题目详情和提交记录。
5. 接入 RabbitMQ，提交后创建判题任务。
6. 实现 Java Judge Worker 的任务消费和结果回写。
7. 实现 Docker 沙箱编译运行 C 代码。
8. 实现考试模式。
9. 补齐限流、审计、异常重试和基础监控。
