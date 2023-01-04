package com.example.fiforum.service.serviceImpl;

import com.example.fiforum.entity.LoginTicket;
import com.example.fiforum.entity.User;
import com.example.fiforum.mapper.UserMapper;
import com.example.fiforum.service.UserService;
import com.example.fiforum.util.CommunityConstant;
import com.example.fiforum.util.CommunityUtil;
import com.example.fiforum.util.MailClient;
import com.example.fiforum.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService, CommunityConstant {
    @Resource
    private UserMapper userMapper;

    @Resource
    private MailClient mailClient;

    @Resource
    private TemplateEngine templateEngine;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 根据id查询用户, 首先从redis缓存中查
     * @param id id
     * @return User
     */
    @Override
    public User findUserById(Integer id) {
        User user = getCache(id);
        if(user == null){
            //缓存中没有则新增
            user = initCache(id);
        }
        return user;
    }

    /**
     * 从redis缓存中获取user对象
     * @param userId id
     * @return user
     */
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 缓存中没有该用户信息时，则将其存入缓存
     * @param userId id
     * @return User
     */
    private User initCache(int userId) {
        //从mapper层获取user, 初始化缓存
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        //TODO user未序列化?
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    /**
     * 用户信息变更时清除对应缓存数据
     * @param userId id
     */
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }


    /**
     * 根据 username 查询用户
     * @param name 用户名
     * @return User
     */
    @Override
    public User findUserByName(String name) {
        return userMapper.selectByName(name);
    }

    /**
     * 用户注册
     * @param user 前端传入的user对象
     * @return 自行封装的map结果, 错误则返回消息提示, 正确则为空.
     */
    @Override
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        //1. 验证输入是否为空
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }

        //2. 验证账号是否存在, 邮箱是否被注册
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册");
            return map;
        }

        //3. 注册用户
        //盐值是随机生成的5位字符, 拼接password进行md5加密存入数据库
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt())); // 加盐加密
        user.setType(0); // 默认普通用户
        user.setStatus(0); // 默认未激活
        user.setActivationCode(CommunityUtil.generateUUID());
        // 随机头像（用户登录后可以自行修改）
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //4. 给用户发送激活邮箱
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //TODO http://localhost:8080/echo/activation/用户id/激活码
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(),"激活 Echo 账号", content);
        return map;
    }

    /**
     * 激活用户
     * @param userId 用户 id
     * @param code 激活码
     * @return 激活状态
     */
    @Override
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            //用户已经激活
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            // 修改用户状态为已激活, 用户信息变更, 清除缓存中的旧数据
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 用户登录（为用户创建凭证）
     * @param username 用户名
     * @param password 密码
     * @param expiredSeconds 多少秒后凭证过期
     * @return Map<String, Object> 返回错误提示消息, 如果成功返回ticket(凭证)
     */
    @Override
    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();
        // 1. 依次验证空, 账号不存在, 账号未激活, 密码错误
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在");
            return map;
        }
        if (user.getStatus() == 0) {
            // 账号未激活
            map.put("usernameMsg", "该账号未激活");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码错误");
            return map;
        }

        // 2. 验证成功, 用户名和密码均正确，为该用户生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID()); // 随机凭证
        loginTicket.setStatus(0); // 设置凭证状态为有效（当用户登出的时候，设置凭证状态为无效）
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L)); // 设置凭证到期时间

        // 3. 将登录凭证存入 redis
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);
        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    /**
     * 用户退出（将凭证状态设为无效）
     * @param ticket 32位随机凭证
     */
    @Override
    public void logout(String ticket) {
        // loginTicketMapper.updateStatus(ticket, 1);
        // 修改（先删除再插入）对应用户在 redis 中的凭证状态, 直接更新redis中的ticket状态
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        assert loginTicket != null;
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    /**
     * 根据 ticket 查询 LoginTicket 信息
     * @param ticket 凭证
     * @return LoginTicket对象
     */
    @Override
    public LoginTicket findLoginTicket(String ticket) {
        // return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 修改用户头像
     * @param userId 用户id
     * @param headUrl 头像url
     * @return 状态码
     */
    @Override
    public int updateHeader(int userId, String headUrl) {
        // return userMapper.updateHeader(userId, headUrl);
        int rows = userMapper.updateHeader(userId, headUrl);
        clearCache(userId);
        return rows;
    }

    /**
     * 修改密码, 盐值不变.
     * @param userId id
     * @param newPassword 新密码
     * @return 状态码
     */
    @Override
    public int updatePassword(int userId, String newPassword){
        User user = userMapper.selectById(userId);
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        clearCache(userId);
        return userMapper.updatePassword(userId, newPassword);
    }

    /**
     * 获取某个用户的权限
     * @param userId id
     * @return TODO 返回Spring Security对象
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add((GrantedAuthority) () -> switch (user.getType()) {
            case 1 -> AUTHORITY_ADMIN;
            case 2 -> AUTHORITY_MODERATOR;
            default -> AUTHORITY_USER;
        });
        return list;
    }


    /**
     * 发送邮箱验证码, 重设密码
     * @param account 账户名, 目前是用户名
     *
     * @return Map<String, Object> 返回错误提示消息，如果返回的 map 为空，则说明发送验证码成功
     */
    @Override
    public Map<String, Object> doSendEmailCode4ResetPwd(String account) {
        Map<String, Object> map = new HashMap<>(2);
        User user = userMapper.selectByName(account);
        if (user == null) {
            map.put("errMsg", "未发现账号");
            return map;
        }
        final String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            map.put("errMsg", "该账号未绑定邮箱");
            return map;
        }
        // 生成6位验证码 给注册用户发送激活邮件
        String randomCode = CommunityUtil.getRandomCode(6);
        Context context = new Context();
        context.setVariable("email", "您的验证码是 " + randomCode);
        // http://localhost:8080/echo/activation/用户id/激活码
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(email,"重置 Echo 账号密码", content);
        //将验证码存入Redis, 十分钟过期
        final String redisKey = "EmailCode4ResetPwd:" + account;
        redisTemplate.opsForValue().set(redisKey, randomCode, 600, TimeUnit.SECONDS);
        return map;
    }

    /**
     * 通过验证码重设密码
     * @param account 账户名, 目前是用户名
     *
     * @return Map<String, Object> 返回错误提示消息，如果返回的 map 为空，则说明发送验证码成功
     */
    @Override
    public Map<String, Object> doResetPwd(String account, String password) {
        Map<String, Object> map = new HashMap<>(2);
        if (StringUtils.isBlank(password)) {
            map.put("errMsg", "密码不能为空");
            return map;
        }
        User user = userMapper.selectByName(account);
        if (user == null) {
            map.put("errMsg", "未发现账号");
            return map;
        }
        final String passwordEncode = CommunityUtil.md5(password + user.getSalt());
        int i = userMapper.updatePassword(user.getId(), passwordEncode);
        if (i <= 0) {
            map.put("errMsg", "修改数据库密码错误");
        } else {
            clearCache(user.getId());
        }
        return map;
    }


}
