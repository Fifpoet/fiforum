package com.example.fiforum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
public class SwaggerConfig {
    @Bean
    Docket docket(){
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(new ApiInfoBuilder()
                        .title("fiforum API Document")
                        .version("v1.0.0")
                        .description("fifpoet论坛的接口文档")
                        .contact(new Contact("fifpoet", "xxx.com", "2854685185@qq.com"))
                        .build())
                .select()
                //设置API扫描路径
                .apis(RequestHandlerSelectors.basePackage("com.example.fiforum.controller"))
                .build();
    }
}
