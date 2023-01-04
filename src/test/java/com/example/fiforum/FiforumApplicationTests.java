package com.example.fiforum;

import com.example.fiforum.entity.User;
import com.example.fiforum.mapper.UserMapper;
import com.example.fiforum.util.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class FiforumApplicationTests {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    UserMapper userMapper;

    @Test
    void contextLoads() {
        int userId = 1;
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        //TODO user未序列化?
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        System.out.println(redisTemplate.opsForValue().get(userId));

    }

}
