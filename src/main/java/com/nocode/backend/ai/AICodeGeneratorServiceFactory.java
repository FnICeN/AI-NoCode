package com.nocode.backend.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nocode.backend.ai.tools.*;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import com.nocode.backend.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AICodeGeneratorServiceFactory {
    @Resource
    private ChatModel chatModel;
    @Resource
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    // 可以认为就是Map
    private final Cache<String, AICodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存），兼容老逻辑
     */
    public AICodeGeneratorService getAICodeGeneratorService(long appId) {
        // 如果使用了这个方法获取Service，那么就会使用chat模型生成。这里填HTML还是MULTI_FILE都没有区别
        return getAICodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AICodeGeneratorService getAICodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 找缓存，否则现场生成
        // 如果找到了缓存，那么首先AIService是缓存里的，而它内部的chatMemory则是Redis里的，每次使用AIService其实都是在读取Redis的数据
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createNewAICodeGeneratorService(appId, codeGenType));
    }

    /**
     * AI Service 创建工厂，根据业务类型不同返回以appId区分的独立Service
     *
     * @param appId       应用ID
     * @param codeGenType 生成类型
     * @return AIService对象
     */
    public AICodeGeneratorService createNewAICodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
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
        // 构建不同的AiService(chat、reason工具调用)
        return switch (codeGenType) {
            // Vue 项目生成，使用工具调用和推理模型（当前暂用chat模型）
            case VUE_PROJECT -> AiServices.builder(AICodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemory(chatMemory)
                    .chatMemoryProvider(memoryId -> chatMemory)  // 框架规定使用@MemoryId时必须使用这个方法
                    .tools(toolManager.getAllTools())
                    //  当调用的tool不存在时的处理策略
                    .hallucinatedToolNameStrategy(request ->
                            ToolExecutionResultMessage.from(request, "没有工具：" + request.name()))
                    .build();
            // 原生 HTML 或多文件生成，使用chat模型
            case HTML, MULTI_FILE -> AiServices.builder(AICodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,"不支持的生成类型：" + codeGenType.getValue());
        };
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

    /**
     * 构造 Cache 的 key
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
