package com.nocode.backend.core;

import com.nocode.backend.ai.AICodeGeneratorService;
import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * AI代码生成门面类，组合代码生成和保存功能
 */
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
            case HTML -> generateANdSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    private File generateANdSaveHtmlCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }
    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileResult(multiFileCodeResult);
    }
}
