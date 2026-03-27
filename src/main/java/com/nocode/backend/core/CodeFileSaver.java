package com.nocode.backend.core;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class CodeFileSaver {
    private static final String FILE_SAVE_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存HTML网页代码
     *
     * @param htmlCodeResult
     * @return
     */
    public static File saveHtmlCodeResult(HtmlCodeResult htmlCodeResult) {
        String dirPath = buildUniqueDir(CodeGenTypeEnum.HTML.getValue());
        saveFile(dirPath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(dirPath);
    }

    /**
     * 保存多文件网页代码
     *
     * @param multiFileCodeResult
     * @return
     */
    public static File saveMultiFileResult(MultiFileCodeResult multiFileCodeResult) {
        String dirPath = buildUniqueDir(CodeGenTypeEnum.MULTI_FILE.getValue());
        saveFile(dirPath, "index.html", multiFileCodeResult.getHtmlCode());
        saveFile(dirPath, "style.css", multiFileCodeResult.getCssCode());
        saveFile(dirPath, "script.js", multiFileCodeResult.getJsCode());
        return new File(dirPath);
    }

    /**
     * 构建文件唯一路径：tmp/code_output/{bizType}_{snowFlakeId}
     * @param bizType 代码生成类型
     * @return
     */
    private static String buildUniqueDir(String bizType) {
        String uniqueFileName = StrUtil.format("{}_{}", bizType, IdUtil.getSnowflakeNextIdStr());
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
    private static void saveFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
    }
}
