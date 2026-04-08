package com.nocode.backend.service.impl;

import com.nocode.backend.innerservice.InnerScreenshotService;
import com.nocode.backend.service.ScreenshotService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class InnerScreenshotServiceImpl implements InnerScreenshotService {

    @Resource
    private ScreenshotService screenshotService;

    @Override
    public String generateAndUpload(String webUrl) {
        return screenshotService.generateAndUpload(webUrl);
    }
}
