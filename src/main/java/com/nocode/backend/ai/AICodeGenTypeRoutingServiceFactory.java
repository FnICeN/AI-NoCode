package com.nocode.backend.ai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AICodeGenTypeRoutingServiceFactory {
    @Resource
    private ChatModel chatModel;
    @Bean
    public AICodeGenTypeRoutingService createAICodeGenTypeRoutingService() {
        return AiServices.builder(AICodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}
