package com.nocode.backend.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

public class CacheKeyUtil {
    /**
     * 根据对象生成缓存键
     *
     * @param object 缓存对象
     * @return 缓存键（MD5）
     */
    public static String generateKey(Object object) {
        if (object == null)
            return DigestUtil.md5Hex("null");
        String jsonStr = JSONUtil.toJsonStr(object);
        return DigestUtil.md5Hex(jsonStr);
    }
}
