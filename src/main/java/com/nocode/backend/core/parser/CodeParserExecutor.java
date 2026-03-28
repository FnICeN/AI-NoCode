package com.nocode.backend.core.parser;

import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器
 *
 */
public class CodeParserExecutor {
    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    public static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();
    /**
     * 执行代码解析
     * @param codeContent 原始代码
     * @param codeGenTypeEnum 解析类型
     * @return 解析结果，如HtmlCodeReslt
     */
    // 返回Object，因为不确定解析出的是哪种Result
    public static Object executeParser(String codeContent, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        };
    }
}
