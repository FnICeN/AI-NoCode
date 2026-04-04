package com.nocode.backend.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具管理器
 */
@Slf4j
@Component
public class ToolManager {
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    @Resource
    BaseTool[] tools;

    /**
     * 初始化工具
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("初始化工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具初始化完成，共 {} 个工具", toolMap.size());
    }

    /**
     * 获取工具
     *
     * @param toolName 工具名称
     * @return 工具
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 获取已注册的所有工具
     * @return 工具实例集合
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}
