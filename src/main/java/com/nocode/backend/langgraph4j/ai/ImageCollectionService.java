package com.nocode.backend.langgraph4j.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;


/**
 * 图片收集 AI 服务接口
 * 使用 AI 调用工具收集不同类型的图片资源
 */
public interface ImageCollectionService {

    /**
     * 根据用户提示词收集所需的图片资源
     * AI 会根据需求自主选择调用相应的工具
     */
    @SystemMessage(fromResource = "prompt/image-collection.txt")
    // 如果的是List<Pojo>而不是Pojo或Flux等，所以需要特殊处理，开启JSON schema
    // 但由于测试使用的模型DeepSeek不支持，所以暂且返回String，之后直接将这个String放在增强提示词中交给AI
    String collectImages(@UserMessage String userPrompt);
}
