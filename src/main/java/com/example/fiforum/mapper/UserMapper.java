package com.example.fiforum.mapper;

import com.example.fiforum.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    /**
     * 根据 id 查询用户
     * @param id id
     * @return User
     */
    User selectById (int id);

    /**
     * 根据 username 查询用户
     * @param username 用户名
     * @return User
     */
    User selectByName(String username);

    /**
     * 根据 email 查询用户
     * @param email 邮件
     * @return User
     */
    User selectByEmail(String email);

    /**
     * 插入用户（注册）
     * @param user user json对象
     * @return code
     */
    int insertUser(User user);

    /**
     * 修改用户状态
     * @param id id
     * @param status 0：未激活，1：已激活
     * @return code
     */
    int updateStatus(int id, int status);

    /**
     * 修改头像
     * @param id id
     * @param headerUrl 头像url
     * @return code
     */
    int updateHeader(int id, String headerUrl);

    /**
     * 修改密码
     * @param id id
     * @param password 新密码
     * @return code
     */
    int updatePassword(int id, String password);
}
