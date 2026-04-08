package com.nocode.backend.core.saver;

import cn.hutool.core.util.StrUtil;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;

public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        saveFile(baseDirPath, "index.html", result.getHtmlCode());
        saveFile(baseDirPath, "style.css", result.getCssCode());
        saveFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "多文件模式至少要有HTML代码");
        }
    }
}
