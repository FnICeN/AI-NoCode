package com.nocode.backend;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.nocode.backend.mapper")
@ComponentScan("com.nocode")
public class NoCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(NoCodeUserApplication.class, args);
    }
}
