package com.nocode.backend.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AICodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;

    /**
     * 创建AI代码生成器Service
     *
     * @return Service对象，可直接chat
     */
    @Bean
    public AICodeGeneratorService createAICodeGeneratorService() {
        return AiServices.create(AICodeGeneratorService.class, chatModel);
    }
}
