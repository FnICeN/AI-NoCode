package com.nocode.backend.core;

import com.nocode.backend.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AICodeGeneratorFacadeTest {
    @Resource
    private AICodeGeneratorFacade facade;

    @Test
    void generateAndSaveCode() {
        File file = facade.generateAndSaveCode("请帮我生成一个简单的登录页面，不多于30行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = facade.generateAndSaveCodeStream("请帮我生成一个简单的登录页面，不多于30行", CodeGenTypeEnum.MULTI_FILE);
        // 等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        Assertions.assertNotNull(result);
        String complete = String.join("", result);
        Assertions.assertNotNull(complete);
    }
}