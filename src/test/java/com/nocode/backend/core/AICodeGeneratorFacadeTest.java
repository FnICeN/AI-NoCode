package com.nocode.backend.core;

import com.nocode.backend.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
@SpringBootTest
class AICodeGeneratorFacadeTest {
    @Resource
    private AICodeGeneratorFacade facade;

    @Test
    void generateAndSaveCode() {
        File file = facade.generateAndSaveCode("请帮我生成一个简单的登录页面，不多于30行", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }
}