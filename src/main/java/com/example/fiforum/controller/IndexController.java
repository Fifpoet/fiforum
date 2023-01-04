package com.example.fiforum.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 首页跳转
 */
@Controller
public class IndexController {
    /**
     * 根路径重定向到index
     * @return model
     */
    @GetMapping("/")
    public String root(){
        return "forward:/index";
    }

    /**
     * TODO 加入首页列表分页
     * @return model
     */
    @GetMapping("/index")
    public String mapToIndex(){
        return "index";
    }
}
