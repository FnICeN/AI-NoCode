package com.nocode.backend.core.saver;

import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;

import java.io.File;

public class CodeFileSaverExecutor {
    private static final HtmlCodeFileSaverTemplate HTML_CODE_FILE_SAVER_TEMPLATE = new HtmlCodeFileSaverTemplate();
    private static final MultiFileCodeFileSaverTemplate MULTI_FILE_CODE_FILE_SAVER = new MultiFileCodeFileSaverTemplate();

    //codeResult 选 Object，因为不确定会是 HtmlCodeResult 还是 MultiFileCodeResult
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        return switch (codeGenTypeEnum) {
            case HTML -> HTML_CODE_FILE_SAVER_TEMPLATE.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> MULTI_FILE_CODE_FILE_SAVER.saveCode((MultiFileCodeResult) codeResult, appId);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码保存类型");
        };
    }
}
