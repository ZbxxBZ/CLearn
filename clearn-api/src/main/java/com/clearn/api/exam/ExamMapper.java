package com.clearn.api.exam;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ExamMapper {

    @Select("""
            select id,
                   title,
                   description,
                   start_time as startTime,
                   end_time as endTime,
                   enabled
            from exams
            order by id desc
            """)
    List<Exam> findAll();

    @Select("""
            select id,
                   title,
                   description,
                   start_time as startTime,
                   end_time as endTime,
                   enabled
            from exams
            where enabled = true
              and start_time <= #{now}
              and end_time >= #{now}
            order by start_time, id
            """)
    List<Exam> findOpenExams(@Param("now") java.time.LocalDateTime now);

    @Select("""
            select id,
                   title,
                   description,
                   start_time as startTime,
                   end_time as endTime,
                   enabled
            from exams
            where id = #{id}
              and enabled = true
            """)
    Exam findEnabledById(Long id);

    @Select("""
            select id,
                   title,
                   description,
                   start_time as startTime,
                   end_time as endTime,
                   enabled
            from exams
            where id = #{id}
            """)
    Exam findById(Long id);

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

    @Insert("""
            insert into exams (
                title,
                description,
                start_time,
                end_time,
                enabled
            )
            values (
                #{title},
                #{description},
                #{startTime},
                #{endTime},
                #{enabled}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Exam exam);

    @Update("""
            update exams
            set title = #{title},
                description = #{description},
                start_time = #{startTime},
                end_time = #{endTime},
                enabled = #{enabled}
            where id = #{id}
            """)
    int update(Exam exam);

    @Update("""
            update exams
            set enabled = false
            where id = #{id}
            """)
    int disableById(Long id);

    @Insert("""
            insert into exam_problems (
                exam_id,
                problem_id,
                score,
                sort_order
            )
            values (
                #{examId},
                #{problemId},
                #{score},
                #{sortOrder}
            )
            """)
    int insertExamProblem(ExamProblem examProblem);

    @Select("""
            select ep.problem_id as problemId,
                   p.title,
                   ep.score as maxScore,
                   case when best.status = 'AC' then ep.score else 0 end as score,
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
                  order by case when s.status = 'AC' then ep.score else 0 end desc,
                           s.id desc
                  limit 1
              )
            where ep.exam_id = #{examId}
              and p.enabled = true
            order by ep.sort_order, ep.id
            """)
    List<ExamProblemScoreRow> findMyProblemScores(@Param("examId") Long examId, @Param("userId") Long userId);

    @Select("""
            select u.id as userId,
                   u.username,
                   ep.problem_id as problemId,
                   p.title,
                   ep.score as maxScore,
                   case when best.status = 'AC' then ep.score else 0 end as score,
                   best.id as bestSubmissionId,
                   best.status as bestStatus,
                   ep.sort_order as sortOrder
            from users u
            join (
                select distinct user_id
                from submissions
                where exam_id = #{examId}
            ) participants on participants.user_id = u.id
            join exam_problems ep on ep.exam_id = #{examId}
            join problems p on p.id = ep.problem_id
            left join submissions best
              on best.id = (
                  select s.id
                  from submissions s
                  where s.exam_id = ep.exam_id
                    and s.problem_id = ep.problem_id
                    and s.user_id = u.id
                  order by case when s.status = 'AC' then ep.score else 0 end desc,
                           s.id desc
                  limit 1
              )
            where p.enabled = true
            order by u.id, ep.sort_order, ep.id
            """)
    List<ExamProblemResultRow> findExamResultRows(Long examId);
}
