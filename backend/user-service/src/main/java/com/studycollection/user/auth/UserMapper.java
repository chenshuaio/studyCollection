package com.studycollection.user.auth;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("select id, username, password_hash as passwordHash, display_name as displayName, role from users where username = #{username}")
    UserEntity findByUsername(String username);

    @Select("select id, username, password_hash as passwordHash, display_name as displayName, role from users where display_name = #{displayName}")
    UserEntity findByDisplayName(String displayName);

    @Insert("""
            insert into users (username, password_hash, display_name, role)
            values (#{username}, #{passwordHash}, #{displayName}, #{role})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserEntity entity);
}
