package com.nocode.backend.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nocode.backend.constant.AppConstant;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * 抽象代码文件保存器 - 模板方法
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {

    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法，保存的标准流程定义
     * @param result 保存文件路径
     * @param appId 应用ID
     * @return
     */
    public final File saveCode(T result, Long appId) {
        validateInput(result);
        String baseDirPath = buildUniqueDir(appId);
        saveFiles(result, baseDirPath);
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果不能为空");
        }
    }

    /**
     * 构建文件唯一路径：tmp/code_output/{bizType}_{snowFlakeId}
     *
     * @param appId 应用ID
     * @return 唯一保存路径
     */
    protected String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }
        String bizType = getCodeType().getValue();
        String uniqueFileName = StrUtil.format("{}_{}", bizType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueFileName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 保存单个文件
     *
     * @param dirPath 目录
     * @param fileName 文件名
     * @param content 内容
     */
    public final void saveFile(String dirPath, String fileName, String content) {
        if (StrUtil.isBlank(dirPath))
            return;
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }

    /**
     * 由子类实现代码类型的选择
     *
     * @return 代码类型
     */
    protected abstract CodeGenTypeEnum getCodeType();

    /**
     * 由子类实现文件保存
     * @param result 代码
     * @param baseDirPath 保存唯一路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
