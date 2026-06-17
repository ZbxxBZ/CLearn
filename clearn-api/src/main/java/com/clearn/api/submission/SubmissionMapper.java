package com.clearn.api.submission;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface SubmissionMapper {

    @Insert("""
            insert into submissions (
                user_id,
                problem_id,
                exam_id,
                language,
                source_code,
                status,
                score,
                passed_test_cases,
                total_test_cases,
                created_at
            )
            values (
                #{userId},
                #{problemId},
                #{examId},
                #{language},
                #{sourceCode},
                #{status},
                #{score},
                #{passedTestCases},
                #{totalTestCases},
                #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Submission submission);

    @Select("""
            select id,
                   user_id as userId,
                   problem_id as problemId,
                   exam_id as examId,
                   language,
                   source_code as sourceCode,
                   status,
                   score,
                   passed_test_cases as passedTestCases,
                   total_test_cases as totalTestCases,
                   time_used_ms as timeUsedMs,
                   memory_used_kb as memoryUsedKb,
                   error_message as errorMessage,
                   created_at as createdAt,
                   judged_at as judgedAt
            from submissions
            where id = #{id}
            """)
    Submission findById(Long id);

    @Select("""
            select id,
                   user_id as userId,
                   problem_id as problemId,
                   exam_id as examId,
                   language,
                   source_code as sourceCode,
                   status,
                   score,
                   passed_test_cases as passedTestCases,
                   total_test_cases as totalTestCases,
                   time_used_ms as timeUsedMs,
                   memory_used_kb as memoryUsedKb,
                   error_message as errorMessage,
                   created_at as createdAt,
                   judged_at as judgedAt
            from submissions
            where id = #{id}
            for update
            """)
    Submission findByIdForUpdate(Long id);

    @Select("""
            select id,
                   user_id as userId,
                   problem_id as problemId,
                   exam_id as examId,
                   language,
                   source_code as sourceCode,
                   status,
                   score,
                   passed_test_cases as passedTestCases,
                   total_test_cases as totalTestCases,
                   time_used_ms as timeUsedMs,
                   memory_used_kb as memoryUsedKb,
                   error_message as errorMessage,
                   created_at as createdAt,
                   judged_at as judgedAt
            from submissions
            where user_id = #{userId}
            order by id desc
            """)
    List<Submission> findByUserId(Long userId);

    @Update("""
            update submissions
            set status = 'JUDGING'
            where id = #{id}
              and status = 'PENDING'
            """)
    int markJudgingIfPending(@Param("id") Long id);

    @Update("""
            update submissions
            set status = #{status},
                score = #{score},
                passed_test_cases = #{passedTestCases},
                total_test_cases = #{totalTestCases},
                time_used_ms = #{timeUsedMs},
                memory_used_kb = #{memoryUsedKb},
                error_message = #{errorMessage},
                judged_at = #{judgedAt}
            where id = #{id}
              and status = 'JUDGING'
            """)
    int finishIfJudging(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("score") Integer score,
            @Param("passedTestCases") Integer passedTestCases,
            @Param("totalTestCases") Integer totalTestCases,
            @Param("timeUsedMs") Integer timeUsedMs,
            @Param("memoryUsedKb") Integer memoryUsedKb,
            @Param("errorMessage") String errorMessage,
            @Param("judgedAt") java.time.LocalDateTime judgedAt
    );

    @Update("""
            update submissions
            set status = 'SE',
                error_message = #{errorMessage},
                judged_at = #{judgedAt}
            where id = #{id}
              and status in ('PENDING', 'JUDGING')
            """)
    int markSystemErrorIfOpen(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("judgedAt") java.time.LocalDateTime judgedAt
    );
}
