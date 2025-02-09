package com.itheima.Mapper;

import com.itheima.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface Test {
    @Insert("insert into user(username, pwd) values(#{username}, #{pwd})")
    int addStudent(UserEntity user);

    @Select("select * from user")
    List<UserEntity> getAllUsers();

    @Select("select * from user where username = #{username}")
    UserEntity getUserById(@Param("username") String username);

}
