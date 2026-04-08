package com.nocode.backend.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.exception.ThrowUtils;
import com.nocode.backend.manager.CosManager;
import com.nocode.backend.service.ScreenshotService;
import com.nocode.backend.utils.WebScreenshotUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {
    @Resource
    private CosManager cosManager;
    @Override
    public String generateAndUpload(String url) {
        ThrowUtils.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "url不能为空");

        log.info("开始截图，URL：{}", url);
        String localScreenshotPath = WebScreenshotUtil.saveWebPageScreenshot(url);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "截图失败");

        try {
            String picUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(picUrl), ErrorCode.OPERATION_ERROR, "上传失败");
            log.info("截图上传成功，URL：{}", picUrl);
            return picUrl;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "截图与上传过程出现异常");
        } finally {
            // 清理截图文件
            cleanUpLocalScreenshot(localScreenshotPath);
        }
    }

    /**
     * 上传截图到对象存储
     *
     * @param localScreenshotPath 本地截图路径
     * @return 对象存储访问URL，失败返回null
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("截图文件不存在: {}", localScreenshotPath);
            return null;
        }
        // 生成 COS 对象键
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        return cosManager.uploadFile(cosKey, screenshotFile);
    }

    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 上传后，删除本地压缩后的截图
     *
     * @param localScreenshotPath
     */
    private void cleanUpLocalScreenshot(String localScreenshotPath) {
        if (StrUtil.isNotBlank(localScreenshotPath)) {
            File screenshotFile = new File(localScreenshotPath);
            if (screenshotFile.exists()) {
                FileUtil.del(screenshotFile);
                log.info("删除本地截图成功：{}", localScreenshotPath);
            }
        }
    }

}
