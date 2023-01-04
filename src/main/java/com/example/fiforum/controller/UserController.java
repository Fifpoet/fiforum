package com.example.fiforum.controller;

import com.example.fiforum.entity.User;
import com.example.fiforum.service.UserService;
import com.example.fiforum.util.HostHolder;
import com.example.fiforum.util.MailClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Controller
@RequestMapping("user")
public class UserController {

    @Resource
    UserService userService;
    
    @Resource
    private HostHolder hostHolder;

    @Resource
    private LikeService likeService;

    @Resource
    private FollowService followService;

    @Resource
    private DiscussPostService discussPostService;

    @Resource
    private CommentService commentService;

    // 网站域名
    @Value("${community.path.domain}")
    private String domain;

    // 项目名(访问路径)
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;


    @GetMapping("find/{id}")
    @ResponseBody
    public User find(@PathVariable Integer id){
        return userService.findUserById(id);
    }

    @GetMapping("hello")
    @ResponseBody
    public void hello(){
        mailClient.sendMail("2854685185@qq.com", "测试", "你好, 王春鑫, 给老子验证码");
    }
}
