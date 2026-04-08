package com.nocode.backend.ai;

import com.nocode.backend.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

public interface AICodeGenTypeRoutingService {
    /**
     * 路由代码生成类型
     * @param userMessage 用户需求
     * @return 推荐代码生成类型
     */
    @SystemMessage(fromResource = "prompt/router.txt")
    CodeGenTypeEnum routeCodeGenType(String userMessage);
}
