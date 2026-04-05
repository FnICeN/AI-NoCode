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
            log.info("图片收集完成，图片列表: {}\n\n", imageListStr);
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
