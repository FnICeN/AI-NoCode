package com.nocode.backend.core;

import com.nocode.backend.ai.AICodeGeneratorService;
import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
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
    private AICodeGeneratorService aiCodeGeneratorService;

    /**
     * 根据类型生成并保存代码
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        return switch (codeGenType) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    /**
     * 根据类型生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        return switch (codeGenType) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // 定义字符串拼接器，拼接所有流式返回用于之后解析保存
        StringBuilder sb = new StringBuilder();
        return result.doOnNext(chunk -> {
            sb.append(chunk);
        }).doOnComplete(() -> {
            try {
                File saveDir = CodeFileSaver.saveHtmlCodeResult(CodeParser.parseHtmlCode(sb.toString()));
                log.info("单HTML文件保存完成，目录：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("单HTML文件保存失败，{}", e.getMessage());
            }
        });
    }

    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        // 定义字符串拼接器，拼接所有流式返回用于之后解析保存
        StringBuilder sb = new StringBuilder();
        return result.doOnNext(chunk -> {
            sb.append(chunk);
        }).doOnComplete(() -> {
            try {
                File saveDir = CodeFileSaver.saveMultiFileResult(CodeParser.parseMultiFileCode(sb.toString()));
                log.info("多文件保存完成，目录：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("多文件保存失败，{}", e.getMessage() );
            }
        });

    }

    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileResult(multiFileCodeResult);
    }
}
