package com.example.fiforum.service;

import com.example.fiforum.entity.LoginTicket;
import com.example.fiforum.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public interface UserService {
    User findUserById(Integer id);
    User findUserByName(String name);
    Map<String, Object> register(User user);
    int activation(int userId, String code);
    Map<String, Object> doResetPwd(String account, String password);
    Map<String, Object> doSendEmailCode4ResetPwd(String account);
    Collection<? extends GrantedAuthority> getAuthorities(int userId);
    int updatePassword(int userId, String newPassword);
    Map<String, Object> login(String username, String password, int expiredSeconds);
    void logout(String ticket);
    LoginTicket findLoginTicket(String ticket);
    int updateHeader(int userId, String headUrl);

}
