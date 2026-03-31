package com.nocode.backend.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nocode.backend.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AICodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    // 可以认为就是Map
    private final Cache<Long, AICodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AICodeGeneratorService getAICodeGeneratorService(long appId) {
        // 找缓存，否则现场生成
        // 如果找到了缓存，那么首先AIService是缓存里的，而它内部的chatMemory则是Redis里的，每次使用AIService其实都是在读取Redis的数据
        return serviceCache.get(appId, this::createNewAICodeGeneratorService);
    }
    
    /**
     * AI Service 创建工厂，根据appId不同返回不同Service
     * @param appId 应用ID
     * @return AIService对象
     */
    public AICodeGeneratorService createNewAICodeGeneratorService(long appId) {
        log.info("未找到Service缓存，现场创建...");
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                // 原本按照官方流程，这里应该是调用chatMemoryProvider(memoryId -> ...)，其中memoryId来自AiService接口注册的@MemoryId参数
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库加载到记忆，又因为记忆就是依托于Redis，所以相当于是从数据库写入Redis
        log.info("将应用：{} 对话历史从数据库写入Redis...", appId);
        int count = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return AiServices.builder(AICodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 创建AI代码生成器Service
     *
     * @return Service对象，可直接chat
     */
    @Bean
    public AICodeGeneratorService createAICodeGeneratorService() {
        return getAICodeGeneratorService(0L);
    }
}
