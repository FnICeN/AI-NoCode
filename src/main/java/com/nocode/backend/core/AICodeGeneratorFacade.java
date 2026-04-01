package com.nocode.backend.core;

import com.nocode.backend.ai.AICodeGeneratorService;
import com.nocode.backend.ai.AICodeGeneratorServiceFactory;
import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.core.parser.CodeParserExecutor;
import com.nocode.backend.core.saver.CodeFileSaverExecutor;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，组合代码生成和保存功能
 */
@Slf4j
@Service
public class AICodeGeneratorFacade {
    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 根据类型生成并保存代码
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        // 根据appId获取定制AI Service
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId, codeGenType);
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    /**
     * 根据类型生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        // 这里获得的Service根据传入的codeGenType决定，可能是chat也可能是reason
        // 获取Service时统一调用双参数方法，如果是VUE则正好获得reason模型，如果不是则获得chat模型，正好可以给HTML和MULTI_FILE用
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId, codeGenType);
        return switch (codeGenType) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                Flux<String> result = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                // TODO: 这里临时使用MULTI_FILE的解析与保存用于测试
                //  Service带工具调用和推理模型（当前暂用chat模型）且使用Vue的提示词，但保存的逻辑还是MULTI_FILE
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    /**
     * 通用代码生成与保存（流式）
     * @param codeStream 代码流
     * @param codeGenType 代码类型
     * @param appId 应用ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 定义字符串拼接器，拼接所有流式返回用于之后解析保存
        StringBuilder sb = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            sb.append(chunk);
        }).doOnComplete(() -> {
            try {
                Object parsedResult = CodeParserExecutor.executeParser(sb.toString(), codeGenType);
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("文件保存完成，目录：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("文件保存失败，{}", e.getMessage() );
            }
        });
    }
}
