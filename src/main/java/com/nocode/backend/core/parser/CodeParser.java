package com.nocode.backend.core.parser;

/**
 * 代码解析器策略接口
 *
 * @param <T>
 */
public interface CodeParser<T> {
    /**
     * 解析代码内容
     * @param code 原始代码内容
     * @return 解析后的结果，例如HtmlCodeResult
     */
    T parseCode(String code);
}
