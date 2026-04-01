package com.nocode.backend.core.handler;

import com.nocode.backend.model.entity.ChatHistory;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.enums.ChatHistoryMessageTypeEnum;
import com.nocode.backend.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class SimpleTextStreamHandler {
    public Flux<String> handle(Flux<String> textStream, ChatHistoryService chatHistoryService, long appId, User loginUser) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return textStream.map(chunk -> {
            aiResponseBuilder.append(chunk);
            return chunk;
        }).doOnComplete(() -> {
            // 保存AI历史记录
            chatHistoryService.saveAIMessage(appId, loginUser.getId(), aiResponseBuilder.toString());
        }).doOnError(error -> {
            // 即使回复失败也保存
            String errorMsg = "AI回复失败：" + error.getMessage();
            chatHistoryService.saveMessage(appId, loginUser.getId(), errorMsg, ChatHistoryMessageTypeEnum.AI.getValue());
        });
    }
}
