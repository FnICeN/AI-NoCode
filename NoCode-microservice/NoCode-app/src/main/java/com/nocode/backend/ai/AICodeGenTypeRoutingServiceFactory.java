package com.nocode.backend.ai;

import com.nocode.backend.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AICodeGenTypeRoutingServiceFactory {
    @Bean
    public AICodeGenTypeRoutingService createAICodeGenTypeRoutingService() {
        ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AICodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}
