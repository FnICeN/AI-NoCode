package com.nocode.backend.ai;

import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

// 新版本LangChain4j支持在接口上直接使用@AiService注解，直接生成实现类注册为Bean且而无需编写Factory，但此版本还不支持，需要手动写Factory
public interface AICodeGeneratorService {
    /**
     * 生成HTML代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     */
    @SystemMessage(fromResource = "prompt/html-file-gen.txt")
    HtmlCodeResult generateHtmlCode(@UserMessage String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     */
    @SystemMessage(fromResource = "prompt/multi-file-gen.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成HTML代码（流式）
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     */
    @SystemMessage(fromResource = "prompt/html-file-gen.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param userMessage 用户提示词
     * @return AI输出结果
     */
    @SystemMessage(fromResource = "prompt/multi-file-gen.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}
