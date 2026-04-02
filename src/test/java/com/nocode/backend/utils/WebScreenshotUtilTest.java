package com.nocode.backend.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebScreenshotUtilTest {

    @Test
    void saveWebPageScreenshot() {
        String url = "https://www.baidu.com";
        String path = WebScreenshotUtil.saveWebPageScreenshot(url);
        Assertions.assertNotNull(path);
    }
}