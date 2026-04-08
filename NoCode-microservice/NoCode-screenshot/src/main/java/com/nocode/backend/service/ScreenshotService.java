package com.nocode.backend.service;

public interface ScreenshotService {
    /**
     * 通用截图上传服务
     * @param url 要截图的网页
     * @return 对象存储的访问地址
     */
    String generateAndUpload(String url);
}
