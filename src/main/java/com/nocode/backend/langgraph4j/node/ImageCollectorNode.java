package com.nocode.backend.langgraph4j.node;

import com.nocode.backend.langgraph4j.ai.ImageCollectionService;
import com.nocode.backend.langgraph4j.state.WorkflowContext;
import com.nocode.backend.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;


import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 图片收集");
            // 获取用户提示词
            String userPrompt = context.getOriginalPrompt();
            String imageListStr = "";
            // 调用图片收集服务
            try {
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                imageListStr = imageCollectionService.collectImages(userPrompt);
            } catch (Exception e) {
                log.error("图片收集失败", e);
            }
            // 更新state
            context.setCurrentStep("图片收集");
            log.info("图片收集完成");
            context.setImageListStr("注意：Logo URL中可能有Expire参数或AccessId参数，在生成网页时不可简化，否则会导致Logo无法显示！\n"
                    + imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
