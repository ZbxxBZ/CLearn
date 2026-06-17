# C 语言在线刷题平台实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在当前 Spring Boot 骨架上实现一个可真实部署的 C 语言在线刷题平台 MVP，包含主系统、RabbitMQ 异步任务、独立 Java Judge Worker、Docker 沙箱判题、学生端、管理员端和基础考试模式。

**架构：** 仓库调整为 Maven 多模块：`clearn-common` 放共享枚举和消息契约，`clearn-api` 提供主业务 API 和静态前端，`clearn-worker` 独立消费判题任务并调用 Docker 沙箱。Web 服务只创建判题任务，用户代码只在 Worker 节点执行。

**技术栈：** Java 17、Spring Boot 4、Spring Security、MyBatis、MySQL、Flyway、RabbitMQ、Redis、Docker、JUnit 5、Mockito、MockMvc。

---

## 范围说明

本计划覆盖规格文档中的第一版范围，并按可验证的端到端路径组织：

1. 学生能登录、看题、提交 C 代码。
2. 主系统能保存提交并发送 RabbitMQ 判题任务。
3. Worker 能消费任务、编译运行、比对输出、回写结果。
4. 管理员能维护题目、测试用例和考试。
5. 静态 Web 前端能完成学生端和管理员端的主要操作。

暂不纳入实现的规格项保持不进入任务：多语言、Special Judge、查重、班级课程体系、排行榜、题目批量导入导出、Worker 自动扩缩容、WebSocket。

## 文件结构

### 根目录

- 修改：`pom.xml`
  - 改为 Maven 聚合项目，声明 `clearn-common`、`clearn-api`、`clearn-worker`。
  - 统一 Java 17、Spring Boot、MyBatis、Flyway、测试依赖版本。
- 保留：`docs/superpowers/specs/2026-06-16-c-online-judge-design.md`
- 创建：`docs/superpowers/plans/2026-06-16-c-online-judge-implementation.md`
- 删除：`src/main/java/com/clearn/clearn/CLearnApplication.java`
- 删除：`src/test/java/com/clearn/clearn/CLearnApplicationTests.java`
- 保留：`src/main/resources/application.properties`
  - 根聚合项目不再使用该文件；执行清理任务时移入 `clearn-api` 或删除。

### `clearn-common`

- 创建：`clearn-common/pom.xml`
- 创建：`clearn-common/src/main/java/com/clearn/common/enums/UserRole.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/enums/SubmissionStatus.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/enums/Language.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/enums/JudgeMode.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/judge/JudgeTaskMessage.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/judge/JudgeFinishRequest.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/judge/JudgeSystemErrorRequest.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/web/ApiResponse.java`
- 测试：`clearn-common/src/test/java/com/clearn/common/judge/JudgeTaskMessageTest.java`

### `clearn-api`

- 创建：`clearn-api/pom.xml`
- 创建：`clearn-api/src/main/java/com/clearn/api/ClearnApiApplication.java`
- 创建：`clearn-api/src/main/resources/application.yml`
- 创建：`clearn-api/src/main/resources/db/migration/V1__init_schema.sql`
- 创建：`clearn-api/src/main/resources/db/migration/V2__seed_dev_data.sql`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/SecurityConfig.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/RabbitConfig.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/RedisConfig.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/*`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/*`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/*`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/*`
- 创建：`clearn-api/src/main/java/com/clearn/api/internal/judge/*`
- 创建：`clearn-api/src/main/resources/static/index.html`
- 创建：`clearn-api/src/main/resources/static/assets/app.js`
- 创建：`clearn-api/src/main/resources/static/assets/styles.css`
- 测试：`clearn-api/src/test/java/com/clearn/api/**`

### `clearn-worker`

- 创建：`clearn-worker/pom.xml`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/ClearnWorkerApplication.java`
- 创建：`clearn-worker/src/main/resources/application.yml`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/config/WorkerRabbitConfig.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/JudgeTaskListener.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/JudgeCoordinator.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/SubmissionLoadService.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/WorkspaceFactory.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/GccCompiler.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/DockerCTestRunner.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/OutputComparator.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/client/JudgeResultClient.java`
- 测试：`clearn-worker/src/test/java/com/clearn/worker/**`

---

## 任务 1：改造为 Maven 多模块项目

**文件：**
- 修改：`pom.xml`
- 创建：`clearn-common/pom.xml`
- 创建：`clearn-api/pom.xml`
- 创建：`clearn-worker/pom.xml`
- 移动：`src/main/java/com/clearn/clearn/CLearnApplication.java` 到 `clearn-api/src/main/java/com/clearn/api/ClearnApiApplication.java`
- 移动：`src/test/java/com/clearn/clearn/CLearnApplicationTests.java` 到 `clearn-api/src/test/java/com/clearn/api/ClearnApiApplicationTests.java`

- [ ] **步骤 1：编写聚合构建结构**

根 `pom.xml` 改为 `pom` packaging，并声明模块：

```xml
<packaging>pom</packaging>

<modules>
    <module>clearn-common</module>
    <module>clearn-api</module>
    <module>clearn-worker</module>
</modules>

<properties>
    <java.version>17</java.version>
</properties>
```

- [ ] **步骤 2：创建 API 模块启动类**

```java
package com.clearn.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.clearn")
public class ClearnApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClearnApiApplication.class, args);
    }
}
```

- [ ] **步骤 3：创建 Worker 模块启动类**

```java
package com.clearn.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.clearn")
public class ClearnWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClearnWorkerApplication.class, args);
    }
}
```

- [ ] **步骤 4：运行模块构建**

运行：

```powershell
.\mvnw.cmd -pl clearn-common,clearn-worker test
.\mvnw.cmd -pl clearn-api -DskipTests package
```

预期：`clearn-common` 和 `clearn-worker` 测试通过，`clearn-api` 完成编译打包。API 的数据库和 context 测试在任务 3 配置测试数据源后运行。

- [ ] **步骤 5：记录构建状态**

若当前目录仍不是 Git 仓库，记录“未提交：当前目录无 `.git`”。若已经初始化 Git：

```powershell
git add pom.xml clearn-common clearn-api clearn-worker
git commit -m "chore: split clearn into api and worker modules"
```

---

## 任务 2：建立共享契约和基础测试

**文件：**
- 创建：`clearn-common/src/main/java/com/clearn/common/enums/*.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/judge/*.java`
- 创建：`clearn-common/src/main/java/com/clearn/common/web/ApiResponse.java`
- 测试：`clearn-common/src/test/java/com/clearn/common/judge/JudgeTaskMessageTest.java`

- [ ] **步骤 1：编写判题状态枚举**

```java
package com.clearn.common.enums;

public enum SubmissionStatus {
    PENDING,
    JUDGING,
    AC,
    WA,
    CE,
    TLE,
    MLE,
    RE,
    SE
}
```

- [ ] **步骤 2：编写判题任务消息**

```java
package com.clearn.common.judge;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import java.time.OffsetDateTime;

public record JudgeTaskMessage(
        Long submissionId,
        Long problemId,
        Language language,
        JudgeMode mode,
        Long examId,
        OffsetDateTime createdAt
) {
}
```

- [ ] **步骤 3：编写 JSON 序列化测试**

```java
package com.clearn.common.judge;

import com.clearn.common.enums.JudgeMode;
import com.clearn.common.enums.Language;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JudgeTaskMessageTest {
    @Test
    void serializesSubmissionIdAndMode() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        JudgeTaskMessage message = new JudgeTaskMessage(
                10001L,
                3001L,
                Language.C,
                JudgeMode.PRACTICE,
                null,
                OffsetDateTime.parse("2026-06-16T01:55:00+08:00")
        );

        String json = mapper.writeValueAsString(message);

        assertThat(json).contains("\"submissionId\":10001");
        assertThat(json).contains("\"mode\":\"PRACTICE\"");
    }
}
```

- [ ] **步骤 4：运行 common 测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-common test
```

预期：`JudgeTaskMessageTest` 通过。

- [ ] **步骤 5：提交契约模块**

```powershell
git add clearn-common
git commit -m "feat: add shared judge contracts"
```

若当前目录不是 Git 仓库，跳过提交命令并继续下一任务。

---

## 任务 3：建立数据库结构和测试数据

**文件：**
- 修改：`clearn-api/pom.xml`
- 创建：`clearn-api/src/main/resources/application.yml`
- 创建：`clearn-api/src/test/resources/application-test.yml`
- 创建：`clearn-api/src/main/resources/db/migration/V1__init_schema.sql`
- 创建：`clearn-api/src/main/resources/db/migration/V2__seed_dev_data.sql`
- 测试：`clearn-api/src/test/java/com/clearn/api/db/SchemaMigrationTest.java`

- [ ] **步骤 1：加入 Flyway 和 H2 测试依赖**

`clearn-api/pom.xml` 增加：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **步骤 2：编写核心表结构**

`V1__init_schema.sql` 包含：

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE problems (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    input_description TEXT NOT NULL,
    output_description TEXT NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    tags VARCHAR(500) NOT NULL DEFAULT '',
    time_limit_ms INT NOT NULL,
    memory_limit_mb INT NOT NULL,
    score INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE test_cases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    sample BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_cases_problem FOREIGN KEY (problem_id) REFERENCES problems(id)
);
```

同一迁移中继续创建 `submissions`、`exams`、`exam_problems`，字段按规格文档保持一致。

- [ ] **步骤 3：编写开发种子数据**

`V2__seed_dev_data.sql` 写入一个管理员、一个学生和一道 A+B 示例题。密码哈希使用 BCrypt，执行实现时通过一个临时测试或 Spring Security 工具生成固定哈希。

```sql
INSERT INTO problems
(title, description, input_description, output_description, difficulty, tags, time_limit_ms, memory_limit_mb, score, enabled)
VALUES
('A+B Problem', 'Read two integers and output their sum.', 'Two integers a and b.', 'The sum of a and b.', 'EASY', 'basic,input-output', 1000, 128, 100, TRUE);
```

- [ ] **步骤 4：编写迁移测试**

```java
package com.clearn.api.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SchemaMigrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsCoreTablesAndSeedProblem() {
        Integer problemCount = jdbcTemplate.queryForObject("select count(*) from problems", Integer.class);
        Integer userCount = jdbcTemplate.queryForObject("select count(*) from users", Integer.class);

        assertThat(problemCount).isGreaterThanOrEqualTo(1);
        assertThat(userCount).isGreaterThanOrEqualTo(2);
    }
}
```

- [ ] **步骤 5：运行 API 数据库测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=SchemaMigrationTest test
```

预期：H2 测试环境完成 Flyway 迁移，并能查询种子用户和题目。

- [ ] **步骤 6：提交数据库基础**

```powershell
git add clearn-api/pom.xml clearn-api/src/main/resources clearn-api/src/test
git commit -m "feat: add database schema for online judge"
```

---

## 任务 4：实现登录、角色权限和内部接口鉴权

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/AuthController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/AuthService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/CurrentUser.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/TokenService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/dto/LoginRequest.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/auth/dto/LoginResponse.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/SecurityConfig.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/InternalApiProperties.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/auth/AuthControllerTest.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/config/SecurityConfigTest.java`

- [ ] **步骤 1：编写登录失败测试**

```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthService authService;

    @Test
    void rejectsInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **步骤 2：实现登录请求和响应**

```java
package com.clearn.api.auth.dto;

public record LoginRequest(String username, String password) {
}
```

```java
package com.clearn.api.auth.dto;

public record LoginResponse(String token, String username, String role) {
}
```

- [ ] **步骤 3：实现 SecurityConfig**

核心规则：

```java
.requestMatchers("/api/auth/login", "/", "/index.html", "/assets/**").permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.requestMatchers("/api/internal/**").authenticated()
.anyRequest().authenticated()
```

内部接口使用 `X-Internal-Token` 过滤器鉴权，学生和管理员 token 不能访问 `/api/internal/**`。

- [ ] **步骤 4：运行权限测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=AuthControllerTest,SecurityConfigTest test
```

预期：登录失败返回 401；未认证访问题目提交接口返回 401；学生访问管理员接口返回 403。

- [ ] **步骤 5：提交认证模块**

```powershell
git add clearn-api/src/main/java/com/clearn/api/auth clearn-api/src/main/java/com/clearn/api/config clearn-api/src/test/java/com/clearn/api/auth clearn-api/src/test/java/com/clearn/api/config
git commit -m "feat: add authentication and role security"
```

---

## 任务 5：实现题库和测试用例管理

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/Problem.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/TestCase.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/ProblemMapper.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/TestCaseMapper.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/ProblemService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/ProblemController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/AdminProblemController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/problem/dto/*.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/problem/ProblemServiceTest.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/problem/AdminProblemControllerTest.java`

- [ ] **步骤 1：编写管理员新增题目测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProblemControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void adminCreatesProblem() throws Exception {
        String token = loginAsAdmin();

        mockMvc.perform(post("/api/admin/problems")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Sum",
                                  "description": "Read two integers.",
                                  "inputDescription": "a b",
                                  "outputDescription": "a+b",
                                  "difficulty": "EASY",
                                  "tags": "basic",
                                  "timeLimitMs": 1000,
                                  "memoryLimitMb": 128,
                                  "score": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber());
    }
}
```

- [ ] **步骤 2：实现 ProblemMapper**

```java
@Mapper
public interface ProblemMapper {
    @Select("""
            SELECT id, title, description, input_description, output_description,
                   difficulty, tags, time_limit_ms, memory_limit_mb, score, enabled,
                   created_at, updated_at
            FROM problems
            WHERE enabled = TRUE
            ORDER BY id DESC
            """)
    List<Problem> findEnabled();

    @Select("""
            SELECT id, title, description, input_description, output_description,
                   difficulty, tags, time_limit_ms, memory_limit_mb, score, enabled,
                   created_at, updated_at
            FROM problems
            WHERE id = #{id}
            """)
    Optional<Problem> findById(long id);
}
```

- [ ] **步骤 3：实现学生题目接口**

`GET /api/problems` 返回启用题目的列表摘要，不返回隐藏测试用例。

`GET /api/problems/{id}` 返回题面和公开样例，不返回隐藏用例。

- [ ] **步骤 4：实现管理员测试用例接口**

管理员接口写入 `test_cases`，支持公开样例和隐藏用例：

```java
public record TestCaseCreateRequest(
        String inputData,
        String expectedOutput,
        boolean sample,
        int sortOrder
) {
}
```

- [ ] **步骤 5：运行题库测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=ProblemServiceTest,AdminProblemControllerTest test
```

预期：管理员能创建题目和测试用例；学生查询题目时看不到隐藏用例。

- [ ] **步骤 6：提交题库模块**

```powershell
git add clearn-api/src/main/java/com/clearn/api/problem clearn-api/src/test/java/com/clearn/api/problem
git commit -m "feat: add problem and test case management"
```

---

## 任务 6：实现提交记录和 RabbitMQ 判题任务

**文件：**
- 修改：`clearn-api/pom.xml`
- 创建：`clearn-api/src/main/java/com/clearn/api/config/RabbitConfig.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/Submission.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionMapper.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/JudgeTaskPublisher.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/submission/dto/*.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/submission/SubmissionServiceTest.java`

- [ ] **步骤 1：添加 AMQP 依赖**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

- [ ] **步骤 2：定义队列配置**

```java
@Configuration
public class RabbitConfig {
    public static final String JUDGE_QUEUE = "judge.submission.queue";

    @Bean
    Queue judgeSubmissionQueue() {
        return QueueBuilder.durable(JUDGE_QUEUE).build();
    }
}
```

- [ ] **步骤 3：编写提交创建测试**

```java
@SpringBootTest
@ActiveProfiles("test")
class SubmissionServiceTest {
    @MockBean
    JudgeTaskPublisher publisher;

    @Autowired
    SubmissionService submissionService;

    @Test
    void createsPendingSubmissionAndPublishesJudgeTask() {
        SubmissionCreateCommand command = new SubmissionCreateCommand(
                2L,
                1L,
                null,
                "int main(){return 0;}"
        );

        Long submissionId = submissionService.createPracticeSubmission(command);

        assertThat(submissionId).isPositive();
        verify(publisher).publish(argThat(message ->
                message.submissionId().equals(submissionId)
                        && message.problemId().equals(1L)
                        && message.language() == Language.C
        ));
    }
}
```

- [ ] **步骤 4：实现提交接口**

`POST /api/problems/{id}/submissions` 接收：

```java
public record SubmissionCreateRequest(String sourceCode) {
}
```

服务层校验：

- 题目存在且启用。
- 源码非空。
- 源码长度不超过配置值，例如 64 KB。
- 创建提交记录时状态为 `PENDING`。
- 发送 `JudgeTaskMessage` 到 `judge.submission.queue`。

- [ ] **步骤 5：运行提交测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=SubmissionServiceTest test
```

预期：提交记录创建成功，RabbitMQ 发布器被调用一次。

- [ ] **步骤 6：提交异步判题入口**

```powershell
git add clearn-api/src/main/java/com/clearn/api/config clearn-api/src/main/java/com/clearn/api/submission clearn-api/src/test/java/com/clearn/api/submission
git commit -m "feat: publish judge task on submission"
```

---

## 任务 7：实现 Worker 数据加载、消费和结果回写客户端

**文件：**
- 修改：`clearn-worker/pom.xml`
- 创建：`clearn-worker/src/main/resources/application.yml`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/config/WorkerRabbitConfig.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/JudgeTaskListener.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/JudgeCoordinator.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/judge/SubmissionLoadService.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/client/JudgeResultClient.java`
- 测试：`clearn-worker/src/test/java/com/clearn/worker/judge/JudgeTaskListenerTest.java`

- [ ] **步骤 1：添加 Worker 依赖**

`clearn-worker/pom.xml` 增加 AMQP、MyBatis、MySQL、WebClient 或 RestClient 相关依赖，并依赖 `clearn-common`。

- [ ] **步骤 2：编写监听器测试**

```java
class JudgeTaskListenerTest {
    @Test
    void delegatesMessageToCoordinator() {
        JudgeCoordinator coordinator = mock(JudgeCoordinator.class);
        JudgeTaskListener listener = new JudgeTaskListener(coordinator);
        JudgeTaskMessage message = new JudgeTaskMessage(1L, 2L, Language.C, JudgeMode.PRACTICE, null, OffsetDateTime.now());

        listener.handle(message);

        verify(coordinator).judge(message);
    }
}
```

- [ ] **步骤 3：实现 RabbitMQ 监听器**

```java
@Component
public class JudgeTaskListener {
    private final JudgeCoordinator coordinator;

    public JudgeTaskListener(JudgeCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @RabbitListener(queues = "judge.submission.queue")
    public void handle(JudgeTaskMessage message) {
        coordinator.judge(message);
    }
}
```

- [ ] **步骤 4：实现结果回写客户端**

`JudgeResultClient` 调用：

- `POST /api/internal/judge/submissions/{id}/start`
- `POST /api/internal/judge/submissions/{id}/finish`
- `POST /api/internal/judge/submissions/{id}/system-error`

所有请求带：

```text
X-Internal-Token: <worker internal token>
```

- [ ] **步骤 5：运行 Worker 监听器测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-worker -Dtest=JudgeTaskListenerTest test
```

预期：监听器正确委托给 `JudgeCoordinator`。

- [ ] **步骤 6：提交 Worker 消费骨架**

```powershell
git add clearn-worker
git commit -m "feat: add judge worker message consumer"
```

---

## 任务 8：实现主系统内部判题接口

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/internal/judge/InternalJudgeController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/internal/judge/InternalJudgeService.java`
- 修改：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionMapper.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/internal/judge/InternalJudgeControllerTest.java`

- [ ] **步骤 1：编写内部接口鉴权测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalJudgeControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void rejectsMissingInternalToken() throws Exception {
        mockMvc.perform(post("/api/internal/judge/submissions/1/start"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **步骤 2：实现开始判题接口**

`POST /api/internal/judge/submissions/{id}/start`：

- 只允许 `PENDING` 变更为 `JUDGING`。
- 如果已经是 `JUDGING` 或终态，返回当前状态，不重复覆盖。

- [ ] **步骤 3：实现完成判题接口**

请求体：

```java
public record JudgeFinishRequest(
        SubmissionStatus status,
        Integer score,
        Long timeUsedMs,
        Long memoryUsedKb,
        String errorMessage
) {
}
```

服务层只允许将 `JUDGING` 更新为终态。终态包含 `AC`、`WA`、`CE`、`TLE`、`MLE`、`RE`、`SE`。

- [ ] **步骤 4：运行内部接口测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=InternalJudgeControllerTest test
```

预期：缺少内部 token 返回 401；合法 token 可以开始并完成判题。

- [ ] **步骤 5：提交内部接口**

```powershell
git add clearn-api/src/main/java/com/clearn/api/internal clearn-api/src/test/java/com/clearn/api/internal clearn-api/src/main/java/com/clearn/api/submission
git commit -m "feat: add internal judge result endpoints"
```

---

## 任务 9：实现判题核心：编译、运行、输出比对

**文件：**
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/WorkspaceFactory.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/GccCompiler.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/DockerCTestRunner.java`
- 创建：`clearn-worker/src/main/java/com/clearn/worker/sandbox/OutputComparator.java`
- 修改：`clearn-worker/src/main/java/com/clearn/worker/judge/JudgeCoordinator.java`
- 测试：`clearn-worker/src/test/java/com/clearn/worker/sandbox/OutputComparatorTest.java`
- 测试：`clearn-worker/src/test/java/com/clearn/worker/judge/JudgeCoordinatorTest.java`

- [ ] **步骤 1：编写输出比对测试**

```java
class OutputComparatorTest {
    private final OutputComparator comparator = new OutputComparator();

    @Test
    void ignoresTrailingWhitespace() {
        assertThat(comparator.matches("3\n", "3\n\n")).isTrue();
    }

    @Test
    void preservesInnerWhitespaceDifference() {
        assertThat(comparator.matches("hello world\n", "hello  world\n")).isFalse();
    }
}
```

- [ ] **步骤 2：实现输出比对**

```java
public class OutputComparator {
    public boolean matches(String expected, String actual) {
        return stripTrailingWhitespace(expected).equals(stripTrailingWhitespace(actual));
    }

    private String stripTrailingWhitespace(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
```

- [ ] **步骤 3：实现 GCC 编译器**

`GccCompiler` 在工作目录写入 `main.c`，执行：

```text
gcc -O2 -std=c11 main.c -o main
```

返回对象包含：

- `success`
- `stderr`
- `exitCode`

编译错误摘要限制长度，例如 4000 字符。

- [ ] **步骤 4：实现 Docker C 运行器**

Docker 命令由 Java `ProcessBuilder` 组装，不拼接 shell 字符串：

```text
docker run --rm
  --network none
  --cpus 1
  --memory 128m
  --pids-limit 64
  --read-only
  --tmpfs /tmp:rw,noexec,nosuid,size=16m
  -v <workspace>:/work:ro
  -w /work
  clearn-c-runner:latest
  /work/main
```

输入通过进程 stdin 写入，stdout 和 stderr 分开收集。

- [ ] **步骤 5：编写 Coordinator 单元测试**

```java
class JudgeCoordinatorTest {
    @Test
    void returnsWrongAnswerOnFirstMismatchedCase() {
        SubmissionLoadService loader = fakeLoaderWithOneCase("1 2\n", "3\n");
        GccCompiler compiler = fakeCompilerSuccess();
        DockerCTestRunner runner = fakeRunnerOutput("4\n");
        JudgeResultClient client = mock(JudgeResultClient.class);

        JudgeCoordinator coordinator = new JudgeCoordinator(loader, compiler, runner, new OutputComparator(), client);
        coordinator.judge(messageForSubmission(1L));

        verify(client).finish(eq(1L), argThat(result -> result.status() == SubmissionStatus.WA));
    }
}
```

- [ ] **步骤 6：运行 Worker 判题测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-worker -Dtest=OutputComparatorTest,JudgeCoordinatorTest test
```

预期：AC、WA、CE、RE、TLE 的协调逻辑都有对应测试并通过。

- [ ] **步骤 7：提交判题核心**

```powershell
git add clearn-worker/src/main/java/com/clearn/worker/sandbox clearn-worker/src/main/java/com/clearn/worker/judge clearn-worker/src/test/java/com/clearn/worker
git commit -m "feat: add docker based c judge worker"
```

---

## 任务 10：实现考试模式

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/Exam.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamProblem.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamMapper.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/AdminExamController.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/*.java`
- 修改：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionService.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/exam/ExamServiceTest.java`

- [ ] **步骤 1：编写考试时间校验测试**

```java
@SpringBootTest
@ActiveProfiles("test")
class ExamServiceTest {
    @Autowired
    ExamService examService;

    @Test
    void rejectsSubmissionOutsideExamWindow() {
        assertThatThrownBy(() -> examService.createExamSubmission(
                new ExamSubmissionCommand(2L, 1L, 1L, "int main(){return 0;}")
        )).isInstanceOf(ExamClosedException.class);
    }
}
```

- [ ] **步骤 2：实现管理员考试管理**

管理员接口支持：

- 创建考试。
- 编辑考试。
- 添加考试题目。
- 查看考试结果。

- [ ] **步骤 3：实现学生考试接口**

学生接口支持：

- 查询开放考试。
- 进入考试详情。
- 提交考试题目代码。
- 查看自己的考试结果。

- [ ] **步骤 4：实现考试成绩统计**

第一版规则：

- 每道题取该学生在该考试内最高分。
- `AC` 得满分。
- 非 `AC` 得 0 分。
- 考试总分为各题最高分之和。

- [ ] **步骤 5：运行考试测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=ExamServiceTest test
```

预期：考试开放时间、题目归属、成绩汇总都按规则通过。

- [ ] **步骤 6：提交考试模块**

```powershell
git add clearn-api/src/main/java/com/clearn/api/exam clearn-api/src/test/java/com/clearn/api/exam clearn-api/src/main/java/com/clearn/api/submission
git commit -m "feat: add exam mode"
```

---

## 任务 11：实现静态学生端和管理员端界面

**文件：**
- 创建：`clearn-api/src/main/resources/static/index.html`
- 创建：`clearn-api/src/main/resources/static/assets/app.js`
- 创建：`clearn-api/src/main/resources/static/assets/styles.css`
- 测试：`clearn-api/src/test/java/com/clearn/api/staticapp/StaticAppTest.java`

- [ ] **步骤 1：编写静态资源测试**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StaticAppTest {
    @Autowired
    MockMvc mockMvc;

    @Test
    void servesIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CLearn")));
    }
}
```

- [ ] **步骤 2：实现第一屏为工作台**

`index.html` 首屏包含登录区。登录后按角色切换：

- 学生：题目列表、题目详情、代码编辑区、提交结果、考试入口。
- 管理员：题目管理、测试用例管理、考试管理、提交记录。

不做营销页，不放大段说明文字。

- [ ] **步骤 3：实现 API 客户端**

`app.js` 维护 token：

```javascript
const state = {
  token: localStorage.getItem("clearn_token"),
  role: localStorage.getItem("clearn_role"),
  currentProblemId: null
};

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Content-Type", "application/json");
  if (state.token) {
    headers.set("Authorization", `Bearer ${state.token}`);
  }
  const response = await fetch(path, {...options, headers});
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return response.json();
}
```

- [ ] **步骤 4：实现学生提交轮询**

提交成功后每 1200ms 查询 `/api/submissions/{id}`，状态进入终态后停止轮询。

- [ ] **步骤 5：运行静态资源测试**

运行：

```powershell
.\mvnw.cmd -pl clearn-api -Dtest=StaticAppTest test
```

预期：首页可访问，静态资源响应 200。

- [ ] **步骤 6：提交前端界面**

```powershell
git add clearn-api/src/main/resources/static clearn-api/src/test/java/com/clearn/api/staticapp
git commit -m "feat: add student and admin web app"
```

---

## 任务 12：端到端联调、限流和运行文档

**文件：**
- 创建：`docker-compose.yml`
- 创建：`docker/c-runner/Dockerfile`
- 创建：`docs/run-local.md`
- 修改：`clearn-api/src/main/resources/application.yml`
- 修改：`clearn-worker/src/main/resources/application.yml`
- 创建：`clearn-api/src/main/java/com/clearn/api/rate/SubmissionRateLimitFilter.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/rate/SubmissionRateLimitFilterTest.java`

- [ ] **步骤 1：创建本地依赖编排**

`docker-compose.yml` 提供：

- MySQL
- RabbitMQ
- Redis

`docker/c-runner/Dockerfile` 提供 C 程序运行镜像，镜像中只包含运行 C 可执行文件所需的最小依赖。

- [ ] **步骤 2：实现提交限流**

学生提交接口按用户维度限流，例如每 10 秒最多 3 次。超过限制返回 429。

测试：

```java
@Test
void returnsTooManyRequestsWhenSubmissionLimitExceeded() throws Exception {
    String token = loginAsStudent();
    for (int i = 0; i < 3; i++) {
        postSubmission(token).andExpect(status().isOk());
    }

    postSubmission(token).andExpect(status().isTooManyRequests());
}
```

- [ ] **步骤 3：编写本地运行文档**

`docs/run-local.md` 包含准确命令：

```powershell
docker compose up -d mysql rabbitmq redis
docker build -t clearn-c-runner:latest docker/c-runner
.\mvnw.cmd -pl clearn-api spring-boot:run
.\mvnw.cmd -pl clearn-worker spring-boot:run
```

- [ ] **步骤 4：运行全量测试**

运行：

```powershell
.\mvnw.cmd test
```

预期：所有不依赖 Docker 守护进程的单元测试和集成测试通过。

- [ ] **步骤 5：运行端到端手工验证**

操作顺序：

1. 启动 MySQL、RabbitMQ、Redis。
2. 构建 `clearn-c-runner:latest`。
3. 启动 `clearn-api`。
4. 启动 `clearn-worker`。
5. 用学生账号登录。
6. 打开 A+B 题。
7. 提交能输出正确答案的 C 代码。
8. 等待提交状态变为 `AC`。
9. 提交输出错误的 C 代码。
10. 等待提交状态变为 `WA`。

- [ ] **步骤 6：提交运行配置**

```powershell
git add docker-compose.yml docker docs/run-local.md clearn-api/src/main clearn-worker/src/main clearn-api/src/test
git commit -m "chore: add local runtime and rate limiting"
```

---

## 自检清单

- 规格中的学生端能力由任务 4、5、6、10、11 覆盖。
- 规格中的管理员端能力由任务 4、5、10、11 覆盖。
- 规格中的 RabbitMQ 异步判题由任务 6、7 覆盖。
- 规格中的 Java Judge Worker 由任务 7、9 覆盖。
- 规格中的 Docker 沙箱由任务 9、12 覆盖。
- 规格中的内部接口鉴权由任务 4、8 覆盖。
- 规格中的数据库模型由任务 3 覆盖。
- 规格中的测试策略由每个任务内的测试步骤覆盖。
- 当前目录若未初始化 Git，所有 commit 步骤在执行时记录为未运行，不影响代码实现。

## 执行检查点

建议按以下检查点审查：

1. 任务 1-3 完成后：确认模块结构、数据库迁移和测试数据。
2. 任务 4-6 完成后：确认登录、题库、提交和 RabbitMQ 发布。
3. 任务 7-9 完成后：确认 Worker 能判题并回写结果。
4. 任务 10-11 完成后：确认考试模式和 Web 界面。
5. 任务 12 完成后：确认本地端到端运行和基础限流。
