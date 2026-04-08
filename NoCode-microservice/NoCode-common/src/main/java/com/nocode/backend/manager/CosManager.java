package com.nocode.backend.manager;

import com.nocode.backend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 腾讯云COS操作器
 */
@Component
@ConditionalOnBean(COSClient.class)
@Slf4j
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;
    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest request = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(request);
    }

    /**
     * 上传文件到 COS 并返回 URL
     *
     * @param key
     * @param file
     * @return
     */
    public String uploadFile(String key, File file) {
        PutObjectResult putObjectResult = putObject(key, file);
        if (putObjectResult != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("上传文件 COS 成功：{} -> {}", file.getName(), url);
            return url;
        }
        log.error("上传文件 COS 失败，返回null...");
        return null;
    }

}
