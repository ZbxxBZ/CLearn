package com.clearn.api.problem;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TestCaseMapper {

    @Select("""
            select id,
                   problem_id as problemId,
                   input_data as inputData,
                   expected_output as expectedOutput,
                   sample,
                   sort_order as sortOrder
            from test_cases
            where problem_id = #{problemId}
              and sample = true
            order by sort_order, id
            """)
    List<TestCase> findSamplesByProblemId(Long problemId);

    @Select("""
            select id,
                   problem_id as problemId,
                   input_data as inputData,
                   expected_output as expectedOutput,
                   sample,
                   sort_order as sortOrder
            from test_cases
            where problem_id = #{problemId}
            order by sort_order, id
            """)
    List<TestCase> findByProblemId(Long problemId);

    @Select("""
            select id,
                   problem_id as problemId,
                   input_data as inputData,
                   expected_output as expectedOutput,
                   sample,
                   sort_order as sortOrder
            from test_cases
            where id = #{id}
            """)
    TestCase findById(Long id);

    @Select("""
            select count(*)
            from test_cases
            where problem_id = #{problemId}
              and sort_order = #{sortOrder}
            """)
    int countByProblemIdAndSortOrder(
            @Param("problemId") Long problemId,
            @Param("sortOrder") Integer sortOrder
    );

    @Select("""
            select count(*)
            from test_cases
            where problem_id = #{problemId}
              and sort_order = #{sortOrder}
              and id <> #{id}
            """)
    int countByProblemIdAndSortOrderExcludingId(
            @Param("problemId") Long problemId,
            @Param("sortOrder") Integer sortOrder,
            @Param("id") Long id
    );

    @Insert("""
            insert into test_cases (
                problem_id,
                input_data,
                expected_output,
                sample,
                sort_order
            )
            values (
                #{problemId},
                #{inputData},
                #{expectedOutput},
                #{sample},
                #{sortOrder}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestCase testCase);

    @Update("""
            update test_cases
            set input_data = #{inputData},
                expected_output = #{expectedOutput},
                sample = #{sample},
                sort_order = #{sortOrder}
            where id = #{id}
            """)
    int update(TestCase testCase);

    @Delete("""
            delete from test_cases
            where id = #{id}
            """)
    int deleteById(Long id);
}
