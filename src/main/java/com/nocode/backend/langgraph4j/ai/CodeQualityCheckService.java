package com.nocode.backend.langgraph4j.ai;

import com.nocode.backend.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 代码质量检查服务接口
 */
public interface CodeQualityCheckService {

    /**
     * 检查代码质量
     * AI 会分析代码并返回质量检查结果
     */
    @UserMessage("{{userMessage}}")
    @SystemMessage(fromResource = "prompt/code-quality-check.txt")
    QualityResult checkCodeQuality(@V("userMessage") String codeContent);
}

