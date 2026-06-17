# 考试模式最小闭环实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 实现学生端考试查询、考试内 C 代码提交、`EXAM` 判题消息发布和我的考试成绩汇总。

**架构：** 在 `clearn-api` 新增 `exam` 包承载考试业务边界，复用现有 `submissions.exam_id`、`exams`、`exam_problems` 表和 `SubmissionService` 的提交入库/事务后发布能力。练习提交保持现有行为，考试规则只放在 `ExamService`。

**技术栈：** Java 17、Spring Boot 4、Spring Security、MyBatis、H2 测试库、JUnit 5、Mockito、MockMvc、RabbitMQ 消息契约。

---

## 文件结构

### 新增 `clearn-api/src/main/java/com/clearn/api/exam/`

- 创建：`Exam.java`
  - 映射 `exams` 表基础字段。
- 创建：`ExamProblem.java`
  - 映射考试题目列表和成绩汇总中需要的题目信息。
- 创建：`ExamMapper.java`
  - 查询开放考试、考试详情、考试题目归属、学生考试成绩。
- 创建：`ExamService.java`
  - 校验考试窗口、题目归属和题目启用状态。
  - 调用 `SubmissionService.createExamSubmission(...)` 创建考试提交。
  - 汇总当前学生考试成绩。
- 创建：`ExamController.java`
  - 暴露学生端考试 API。
- 创建：`ExamClosedException.java`
  - 表达考试不在开放窗口内。
- 创建：`ExamProblemScoreRow.java`
  - 映射成绩汇总 SQL 的每题最佳提交结果。

### 新增 `clearn-api/src/main/java/com/clearn/api/exam/dto/`

- 创建：`ExamSummaryResponse.java`
- 创建：`ExamDetailResponse.java`
- 创建：`ExamProblemResponse.java`
- 创建：`ExamResultResponse.java`
- 创建：`ExamProblemResultResponse.java`

### 修改 `clearn-api/src/main/java/com/clearn/api/submission/`

- 修改：`SubmissionService.java`
  - 新增 `createExamSubmission(Long userId, Long examId, Long problemId, SubmissionCreateRequest request)`。
  - 保持 `createPracticeSubmission(...)` 行为不变。
  - 抽取私有 `createSubmission(...)`，统一源码校验、入库、事务后发布。

### 测试

- 创建：`clearn-api/src/test/java/com/clearn/api/exam/ExamServiceTest.java`
- 创建：`clearn-api/src/test/java/com/clearn/api/exam/ExamControllerTest.java`
- 修改：`clearn-api/src/test/java/com/clearn/api/submission/SubmissionServiceTest.java`
  - 如需要，补充考试提交复用提交服务的事务后发布测试。

---

## 任务 1：提交服务支持考试提交

**文件：**
- 修改：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionService.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/submission/SubmissionServiceTest.java`

- [ ] **步骤 1：编写失败测试：考试提交保存 exam_id 并发布 EXAM 消息**

在 `SubmissionServiceTest` 中新增：

```java
@Test
void createExamSubmissionPersistsExamIdAndPublishesExamJudgeTask() {
    Long userId = userId("student");
    Long problemId = seedProblemId();
    Long examId = insertOpenExamWithProblem(problemId, 100);

    SubmissionCreateResponse response = submissionService.createExamSubmission(
            userId,
            examId,
            problemId,
            new SubmissionCreateRequest("int main(void) { return 0; }")
    );

    assertThat(response.submissionId()).isNotNull();
    assertThat(jdbcTemplate.queryForObject(
            """
                    select count(*)
                    from submissions
                    where id = ?
                      and user_id = ?
                      and problem_id = ?
                      and exam_id = ?
                      and status = 'PENDING'
                    """,
            Integer.class,
            response.submissionId(),
            userId,
            problemId,
            examId
    )).isEqualTo(1);

    ArgumentCaptor<JudgeTaskMessage> captor = ArgumentCaptor.forClass(JudgeTaskMessage.class);
    verify(judgeTaskPublisher).publish(captor.capture());
    assertThat(captor.getValue().mode()).isEqualTo(JudgeMode.EXAM);
    assertThat(captor.getValue().examId()).isEqualTo(examId);
}
```

新增测试辅助方法：

```java
private Long insertOpenExamWithProblem(Long problemId, int score) {
    jdbcTemplate.update(
            """
                    insert into exams (title, description, start_time, end_time, enabled)
                    values ('Submission Service Exam', 'Exam.', DATEADD('HOUR', -1, CURRENT_TIMESTAMP),
                            DATEADD('HOUR', 1, CURRENT_TIMESTAMP), true)
                    """
    );
    Long examId = jdbcTemplate.queryForObject(
            "select id from exams where title = 'Submission Service Exam'",
            Long.class
    );
    jdbcTemplate.update(
            "insert into exam_problems (exam_id, problem_id, score, sort_order) values (?, ?, ?, 1)",
            examId,
            problemId,
            score
    );
    return examId;
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api -Dtest=SubmissionServiceTest test
```

预期：编译失败或测试失败，因为 `createExamSubmission` 还不存在。

- [ ] **步骤 3：实现最小提交服务改造**

在 `SubmissionService` 中新增：

```java
@Transactional
public SubmissionCreateResponse createExamSubmission(
        Long userId,
        Long examId,
        Long problemId,
        SubmissionCreateRequest request
) {
    requireId(examId, "examId");
    return createSubmission(userId, problemId, examId, request, JudgeMode.EXAM);
}
```

把 `createPracticeSubmission` 改为：

```java
@Transactional
public SubmissionCreateResponse createPracticeSubmission(
        Long userId,
        Long problemId,
        SubmissionCreateRequest request
) {
    return createSubmission(userId, problemId, null, request, JudgeMode.PRACTICE);
}
```

抽取：

```java
private SubmissionCreateResponse createSubmission(
        Long userId,
        Long problemId,
        Long examId,
        SubmissionCreateRequest request,
        JudgeMode mode
) {
    requireId(userId, "userId");
    requireId(problemId, "problemId");
    String sourceCode = validateSourceCode(request);
    requireEnabledProblem(problemId);

    OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    Submission submission = new Submission();
    submission.setUserId(userId);
    submission.setProblemId(problemId);
    submission.setExamId(examId);
    submission.setLanguage(Language.C.name());
    submission.setSourceCode(sourceCode);
    submission.setStatus(SubmissionStatus.PENDING.name());
    submission.setScore(0);
    submission.setCreatedAt(createdAt.toLocalDateTime());
    submissionMapper.insert(submission);

    JudgeTaskMessage message = new JudgeTaskMessage(
            submission.getId(),
            problemId,
            Language.C,
            mode,
            examId,
            createdAt
    );
    publishAfterCommit(message);

    return new SubmissionCreateResponse(submission.getId(), submission.getStatus());
}
```

- [ ] **步骤 4：运行提交服务测试确认通过**

运行同一步骤 2 命令。

预期：`SubmissionServiceTest` 通过。

---

## 任务 2：实现考试查询和提交业务服务

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/Exam.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamProblem.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamMapper.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamClosedException.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/ExamSummaryResponse.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/ExamDetailResponse.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/ExamProblemResponse.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/exam/ExamServiceTest.java`

- [ ] **步骤 1：编写失败测试：考试窗口外拒绝提交**

`ExamServiceTest`：

```java
@SpringBootTest
@ActiveProfiles("test")
class ExamServiceTest {
    @Autowired
    private ExamService examService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void rejectsSubmissionBeforeExamStarts() {
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Future Exam",
                problemId,
                "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
                "DATEADD('HOUR', 2, CURRENT_TIMESTAMP)",
                true,
                100
        );

        assertThatThrownBy(() -> examService.createExamSubmission(
                userId("student"),
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(ExamClosedException.class)
                .hasMessageContaining("exam is not open");

        verifyNoInteractions(judgeTaskPublisher);
    }

    @Test
    void rejectsSubmissionAfterExamEnds() {
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Past Exam",
                problemId,
                "DATEADD('HOUR', -2, CURRENT_TIMESTAMP)",
                "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
                true,
                100
        );

        assertThatThrownBy(() -> examService.createExamSubmission(
                userId("student"),
                examId,
                problemId,
                new SubmissionCreateRequest("int main(void) { return 0; }")
        ))
                .isInstanceOf(ExamClosedException.class);

        verifyNoInteractions(judgeTaskPublisher);
    }
}
```

测试辅助方法使用 `JdbcTemplate.update("insert into exams ... " + startExpression + ...)` 拼接固定测试表达式，不接收外部输入。

- [ ] **步骤 2：编写失败测试：题目不属于考试时拒绝提交**

新增：

```java
@Test
void rejectsSubmissionWhenProblemIsNotInExam() {
    Long problemId = seedProblemId();
    Long otherProblemId = insertProblem("Exam Other Problem");
    Long examId = insertExamWithProblem(
            "Open Exam",
            problemId,
            "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
            "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
            true,
            100
    );

    assertThatThrownBy(() -> examService.createExamSubmission(
            userId("student"),
            examId,
            otherProblemId,
            new SubmissionCreateRequest("int main(void) { return 0; }")
    ))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("exam problem");

    verifyNoInteractions(judgeTaskPublisher);
}
```

- [ ] **步骤 3：编写失败测试：开放考试提交成功**

新增：

```java
@Test
void createsSubmissionForOpenExamProblem() {
    Long problemId = seedProblemId();
    Long examId = insertExamWithProblem(
            "Current Exam",
            problemId,
            "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
            "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
            true,
            100
    );

    SubmissionCreateResponse response = examService.createExamSubmission(
            userId("student"),
            examId,
            problemId,
            new SubmissionCreateRequest("int main(void) { return 0; }")
    );

    assertThat(response.status()).isEqualTo("PENDING");
    assertThat(jdbcTemplate.queryForObject(
            "select exam_id from submissions where id = ?",
            Long.class,
            response.submissionId()
    )).isEqualTo(examId);
}
```

- [ ] **步骤 4：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api -Dtest=ExamServiceTest test
```

预期：编译失败，因为考试类还不存在。

- [ ] **步骤 5：实现实体、Mapper 和服务最小功能**

`Exam.java` 字段：

```java
private Long id;
private String title;
private String description;
private LocalDateTime startTime;
private LocalDateTime endTime;
private Boolean enabled;
```

`ExamProblem.java` 字段：

```java
private Long examId;
private Long problemId;
private String title;
private String difficulty;
private Integer score;
private Integer sortOrder;
```

`ExamMapper` 方法：

```java
@Select("""
        select id, title, description, start_time as startTime, end_time as endTime, enabled
        from exams
        where enabled = true
          and start_time <= CURRENT_TIMESTAMP
          and end_time >= CURRENT_TIMESTAMP
        order by start_time, id
        """)
List<Exam> findOpenExams();

@Select("""
        select id, title, description, start_time as startTime, end_time as endTime, enabled
        from exams
        where id = #{id}
          and enabled = true
        """)
Exam findEnabledById(Long id);

@Select("""
        select ep.exam_id as examId,
               ep.problem_id as problemId,
               p.title,
               p.difficulty,
               ep.score,
               ep.sort_order as sortOrder
        from exam_problems ep
        join problems p on p.id = ep.problem_id
        where ep.exam_id = #{examId}
          and p.enabled = true
        order by ep.sort_order, ep.id
        """)
List<ExamProblem> findEnabledProblemsByExamId(Long examId);

@Select("""
        select count(*)
        from exam_problems ep
        join problems p on p.id = ep.problem_id
        where ep.exam_id = #{examId}
          and ep.problem_id = #{problemId}
          and p.enabled = true
        """)
int countEnabledExamProblem(@Param("examId") Long examId, @Param("problemId") Long problemId);
```

`ExamService.createExamSubmission(...)`：

```java
public SubmissionCreateResponse createExamSubmission(
        Long userId,
        Long examId,
        Long problemId,
        SubmissionCreateRequest request
) {
    requireId(userId, "userId");
    requireId(examId, "examId");
    requireId(problemId, "problemId");
    Exam exam = requireEnabledExam(examId);
    requireOpen(exam);
    requireExamProblem(examId, problemId);
    return submissionService.createExamSubmission(userId, examId, problemId, request);
}
```

`requireOpen` 使用 `LocalDateTime.now(Clock.systemUTC())` 或注入 `Clock`，推荐构造函数注入 `Clock` 并提供默认 bean。

- [ ] **步骤 6：运行考试服务提交测试确认通过**

运行同一步骤 4 命令。

预期：考试时间、题目归属、提交成功测试通过。

---

## 任务 3：实现考试成绩汇总

**文件：**
- 修改：`clearn-api/src/main/java/com/clearn/api/exam/ExamMapper.java`
- 修改：`clearn-api/src/main/java/com/clearn/api/exam/ExamService.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamProblemScoreRow.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/ExamResultResponse.java`
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/dto/ExamProblemResultResponse.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/exam/ExamServiceTest.java`

- [ ] **步骤 1：编写失败测试：每题取最高分，未提交题为 0**

在 `ExamServiceTest` 新增：

```java
@Test
void myResultUsesBestScorePerProblemAndZeroForUnsubmittedProblems() {
    Long userId = userId("student");
    Long firstProblemId = seedProblemId();
    Long secondProblemId = insertProblem("Second Exam Problem");
    Long thirdProblemId = insertProblem("Third Exam Problem");
    Long examId = insertExamWithProblem(
            "Result Exam",
            firstProblemId,
            "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
            "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
            true,
            100
    );
    addExamProblem(examId, secondProblemId, 50, 2);
    addExamProblem(examId, thirdProblemId, 30, 3);
    insertJudgedSubmission(userId, examId, firstProblemId, "WA", 0);
    Long bestSubmissionId = insertJudgedSubmission(userId, examId, firstProblemId, "AC", 100);
    insertJudgedSubmission(userId, examId, secondProblemId, "WA", 0);

    ExamResultResponse result = examService.getMyResult(userId, examId);

    assertThat(result.totalScore()).isEqualTo(100);
    assertThat(result.maxScore()).isEqualTo(180);
    assertThat(result.problems()).hasSize(3);
    assertThat(result.problems().get(0).score()).isEqualTo(100);
    assertThat(result.problems().get(0).bestSubmissionId()).isEqualTo(bestSubmissionId);
    assertThat(result.problems().get(1).score()).isZero();
    assertThat(result.problems().get(2).score()).isZero();
    assertThat(result.problems().get(2).bestSubmissionId()).isNull();
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api -Dtest=ExamServiceTest test
```

预期：编译失败，因为成绩 DTO 和服务方法还不存在。

- [ ] **步骤 3：实现成绩查询 SQL 和 DTO 映射**

`ExamProblemScoreRow` 字段：

```java
private Long problemId;
private String title;
private Integer maxScore;
private Integer score;
private Long bestSubmissionId;
private String bestStatus;
private Integer sortOrder;
```

`ExamMapper.findMyProblemScores(...)`：

```java
@Select("""
        select ep.problem_id as problemId,
               p.title,
               ep.score as maxScore,
               coalesce(best.score, 0) as score,
               best.id as bestSubmissionId,
               best.status as bestStatus,
               ep.sort_order as sortOrder
        from exam_problems ep
        join problems p on p.id = ep.problem_id
        left join submissions best
          on best.id = (
              select s.id
              from submissions s
              where s.exam_id = ep.exam_id
                and s.problem_id = ep.problem_id
                and s.user_id = #{userId}
              order by s.score desc, s.id desc
              limit 1
          )
        where ep.exam_id = #{examId}
        order by ep.sort_order, ep.id
        """)
List<ExamProblemScoreRow> findMyProblemScores(@Param("examId") Long examId, @Param("userId") Long userId);
```

`ExamService.getMyResult(...)`：

```java
public ExamResultResponse getMyResult(Long userId, Long examId) {
    requireId(userId, "userId");
    requireId(examId, "examId");
    Exam exam = requireEnabledExam(examId);
    List<ExamProblemResultResponse> problems = examMapper.findMyProblemScores(examId, userId)
            .stream()
            .map(this::toProblemResult)
            .toList();
    int totalScore = problems.stream().mapToInt(ExamProblemResultResponse::score).sum();
    int maxScore = problems.stream().mapToInt(ExamProblemResultResponse::maxScore).sum();
    return new ExamResultResponse(exam.getId(), exam.getTitle(), totalScore, maxScore, problems);
}
```

- [ ] **步骤 4：运行成绩测试确认通过**

运行同一步骤 2 命令。

预期：`ExamServiceTest` 通过。

---

## 任务 4：实现学生考试控制器

**文件：**
- 创建：`clearn-api/src/main/java/com/clearn/api/exam/ExamController.java`
- 修改：`clearn-api/src/main/java/com/clearn/api/exam/ExamService.java`
- 测试：`clearn-api/src/test/java/com/clearn/api/exam/ExamControllerTest.java`

- [ ] **步骤 1：编写失败测试：学生查询开放考试和详情**

`ExamControllerTest`：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExamControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JudgeTaskPublisher judgeTaskPublisher;

    @Test
    void studentListsOpenExamsAndReadsDetail() throws Exception {
        String token = loginAndReadToken("student");
        Long problemId = seedProblemId();
        Long examId = insertExamWithProblem(
                "Controller Exam",
                problemId,
                "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
                "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
                true,
                100
        );

        mockMvc.perform(get("/api/exams").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Controller Exam"));

        mockMvc.perform(get("/api/exams/{id}", examId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(examId))
                .andExpect(jsonPath("$.data.problems[0].problemId").value(problemId))
                .andExpect(jsonPath("$.data.problems[0].score").value(100));
    }
}
```

- [ ] **步骤 2：编写失败测试：学生通过控制器创建考试提交并查询结果**

新增：

```java
@Test
void studentCreatesExamSubmissionAndReadsMyResult() throws Exception {
    String token = loginAndReadToken("student");
    Long userId = userId("student");
    Long problemId = seedProblemId();
    Long examId = insertExamWithProblem(
            "Controller Submit Exam",
            problemId,
            "DATEADD('HOUR', -1, CURRENT_TIMESTAMP)",
            "DATEADD('HOUR', 1, CURRENT_TIMESTAMP)",
            true,
            100
    );

    mockMvc.perform(post("/api/exams/{examId}/problems/{problemId}/submissions", examId, problemId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"sourceCode":"int main(void) { return 0; }"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

    Long submissionId = jdbcTemplate.queryForObject(
            "select max(id) from submissions where user_id = ? and exam_id = ? and problem_id = ?",
            Long.class,
            userId,
            examId,
            problemId
    );
    jdbcTemplate.update(
            "update submissions set status = 'AC', score = 100, judged_at = CURRENT_TIMESTAMP where id = ?",
            submissionId
    );

    mockMvc.perform(get("/api/exams/{id}/my-result", examId)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalScore").value(100))
            .andExpect(jsonPath("$.data.problems[0].bestSubmissionId").value(submissionId));
}
```

- [ ] **步骤 3：运行测试确认失败**

运行：

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api -Dtest=ExamControllerTest test
```

预期：编译失败，因为控制器还不存在。

- [ ] **步骤 4：实现控制器**

`ExamController`：

```java
@RestController
@RequestMapping("/api/exams")
public class ExamController {
    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<List<ExamSummaryResponse>> listOpenExams() {
        return ApiResponse.success(examService.listOpenExams());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExamDetailResponse> getExam(@PathVariable Long id) {
        return ApiResponse.success(examService.getExamDetail(id));
    }

    @PostMapping("/{examId}/problems/{problemId}/submissions")
    public ApiResponse<SubmissionCreateResponse> createExamSubmission(
            @PathVariable Long examId,
            @PathVariable Long problemId,
            @RequestBody SubmissionCreateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(examService.createExamSubmission(currentUser.id(), examId, problemId, request));
    }

    @GetMapping("/{id}/my-result")
    public ApiResponse<ExamResultResponse> getMyResult(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ApiResponse.success(examService.getMyResult(currentUser.id(), id));
    }
}
```

异常处理沿用现有控制器风格：

```java
@ExceptionHandler({IllegalArgumentException.class, ExamClosedException.class})
public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(ex.getMessage()));
}

@ExceptionHandler(NoSuchElementException.class)
public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(ex.getMessage()));
}
```

- [ ] **步骤 5：运行控制器测试确认通过**

运行同一步骤 3 命令。

预期：`ExamControllerTest` 通过。

---

## 任务 5：回归验证和收尾审查

**文件：**
- 检查：`clearn-api/src/main/java/com/clearn/api/exam/**`
- 检查：`clearn-api/src/main/java/com/clearn/api/submission/SubmissionService.java`
- 检查：`clearn-api/src/test/java/com/clearn/api/exam/**`
- 检查：`clearn-api/src/test/java/com/clearn/api/submission/SubmissionServiceTest.java`

- [ ] **步骤 1：运行考试相关测试**

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api -Dtest=ExamServiceTest,ExamControllerTest,SubmissionServiceTest test
```

预期：全部通过。

- [ ] **步骤 2：运行 API 模块测试**

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -pl clearn-api test
```

预期：全部通过。

- [ ] **步骤 3：运行全量测试**

```powershell
$env:JAVA_HOME='C:\Users\Administrator\.jdks\ms-17.0.19'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

预期：全部通过。

- [ ] **步骤 4：规格核对**

逐项核对：

- `GET /api/exams` 返回开放考试。
- `GET /api/exams/{id}` 返回考试详情和题目列表。
- `POST /api/exams/{examId}/problems/{problemId}/submissions` 创建 `EXAM` 提交。
- `GET /api/exams/{id}/my-result` 返回每题最高分汇总。
- 考试窗口外拒绝提交。
- 题目不属于考试时拒绝提交。
- 练习提交仍发布 `PRACTICE` 消息且 `exam_id` 为空。

- [ ] **步骤 5：记录 Git 限制**

当前目录不是 Git 仓库，不能执行 commit、SHA diff 或 worktree 清理。收尾报告中明确说明。
