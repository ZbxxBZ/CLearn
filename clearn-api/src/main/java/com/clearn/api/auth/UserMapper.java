package com.clearn.api.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("""
            select id, username, password_hash as passwordHash, role, enabled
            from users
            where username = #{username}
            """)
    UserAccount findByUsername(String username);

    @Select("""
            select id, username, password_hash as passwordHash, role, enabled
            from users
            where id = #{id}
            """)
    UserAccount findById(Long id);
}
