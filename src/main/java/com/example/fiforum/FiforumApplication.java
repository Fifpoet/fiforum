package com.example.fiforum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableOpenApi
@EnableWebMvc
@EnableSwagger2
public class FiforumApplication {

    public static void main(String[] args) {
        SpringApplication.run(FiforumApplication.class, args);
    }

}
