package com.clearn.api.problem;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProblemMapper {

    @Select("""
            select id,
                   title,
                   description,
                   input_description as inputDescription,
                   output_description as outputDescription,
                   difficulty,
                   tags,
                   time_limit_ms as timeLimitMs,
                   memory_limit_mb as memoryLimitMb,
                   score,
                   enabled
            from problems
            where enabled = true
            order by id
            """)
    List<Problem> findEnabledProblems();

    @Select("""
            select id,
                   title,
                   description,
                   input_description as inputDescription,
                   output_description as outputDescription,
                   difficulty,
                   tags,
                   time_limit_ms as timeLimitMs,
                   memory_limit_mb as memoryLimitMb,
                   score,
                   enabled
            from problems
            where id = #{id}
            """)
    Problem findById(Long id);

    @Select("""
            select id,
                   title,
                   description,
                   input_description as inputDescription,
                   output_description as outputDescription,
                   difficulty,
                   tags,
                   time_limit_ms as timeLimitMs,
                   memory_limit_mb as memoryLimitMb,
                   score,
                   enabled
            from problems
            where id = #{id}
              and enabled = true
            """)
    Problem findEnabledById(Long id);

    @Insert("""
            insert into problems (
                title,
                description,
                input_description,
                output_description,
                difficulty,
                tags,
                time_limit_ms,
                memory_limit_mb,
                score,
                enabled
            )
            values (
                #{title},
                #{description},
                #{inputDescription},
                #{outputDescription},
                #{difficulty},
                #{tags},
                #{timeLimitMs},
                #{memoryLimitMb},
                #{score},
                #{enabled}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Problem problem);

    @Update("""
            update problems
            set title = #{title},
                description = #{description},
                input_description = #{inputDescription},
                output_description = #{outputDescription},
                difficulty = #{difficulty},
                tags = #{tags},
                time_limit_ms = #{timeLimitMs},
                memory_limit_mb = #{memoryLimitMb},
                score = #{score},
                enabled = #{enabled},
                updated_at = CURRENT_TIMESTAMP
            where id = #{id}
            """)
    int update(Problem problem);
}
