package com.nocode.backend.ai.tools;

import cn.hutool.json.JSONObject;

/**
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {
    /**
     * 英文工具名称（方法名被LangChain4j注册为工具名）
     *
     * @return 工具名称
     */
    public abstract String getToolName();

    /**
     * 中文显示名称
     * @return 显示名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求响应
     * @return 给用户的工具请求显示文本
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行响应（保存到数据库）
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecuteResponse(JSONObject arguments);
}
