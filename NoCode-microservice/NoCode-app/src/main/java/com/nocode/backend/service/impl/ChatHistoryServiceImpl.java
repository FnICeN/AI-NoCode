package com.nocode.backend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nocode.backend.constant.UserConstant;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.exception.ThrowUtils;
import com.nocode.backend.mapper.ChatHistoryMapper;
import com.nocode.backend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.nocode.backend.model.entity.App;
import com.nocode.backend.model.entity.ChatHistory;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.enums.ChatHistoryMessageTypeEnum;
import com.nocode.backend.model.enums.UserRoleEnum;
import com.nocode.backend.service.AppService;
import com.nocode.backend.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author FICN
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public boolean saveUserMessage(Long appId, Long userId, String message) {
        return saveMessage(appId, userId, message, ChatHistoryMessageTypeEnum.USER.getValue());
    }

    @Override
    public boolean saveAIMessage(Long appId, Long userId, String message) {
        return saveMessage(appId, userId, message, ChatHistoryMessageTypeEnum.AI.getValue());
    }

    @Override
    public boolean saveErrorMessage(Long appId, Long userId, String errorMessage) {
        return saveMessage(appId, userId, errorMessage, ChatHistoryMessageTypeEnum.ERROR.getValue());
    }

    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        return this.page(Page.of(1, pageSize), queryWrapper);
    }


    @Override
    public Page<ChatHistory> listAllChatHistory(int pageSize, long offset) {
        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .orderBy("createTime", false); // 按照时间降序排序

        // 计算页码
        int pageNum = (int) (offset / pageSize) + 1;

        // 执行查询
        return this.page(Page.of(pageNum, pageSize), queryWrapper);
    }

    @Override
    public boolean deleteChatHistoryByAppId(Long appId) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }

        // 构建删除条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);

        // 执行删除
        return this.remove(queryWrapper);
    }

    @Override
    public boolean validatePermission(Long appId, User loginUser) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }

        // 管理员可以查看所有应用的对话历史
        if (UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            return true;
        }

        // 应用创建者可以查看自己应用的对话历史
        App app = appService.getAppById(appId);
        return app.getUserId().equals(loginUser.getId());
    }

    /**
     * 保存消息的通用方法
     *
     * @param appId      应用ID
     * @param userId     用户ID
     * @param message    消息内容
     * @param messageType 消息类型
     * @return 保存是否成功
     */
    public boolean saveMessage(Long appId, Long userId, String message, String messageType) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id不合法");
        }
        if (message == null || message.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        }

        // 验证应用是否存在
        App app = appService.getAppById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 创建对话历史
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(messageType)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDelete(0)
                .build();

        // 保存到数据库
        boolean saveResult = this.save(chatHistory);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存对话历史失败");
        }

        return true;
    }

    /**
     * 将数据库的对话历史加载到 ChatMemory
     * @param appId 应用ID
     * @param chatMemory 对话记忆对象
     * @param maxCount 最大加载条数
     * @return 成功加载条数
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> chatHistoryList = this.list(queryWrapper);
            if (CollUtil.isEmpty(chatHistoryList)) {
                return 0;
            }
            // 反转列表保证正序
            chatHistoryList = chatHistoryList.reversed();
            int loadedCount = 0;
            chatMemory.clear();
            for (ChatHistory chatHistory : chatHistoryList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(UserMessage.from(chatHistory.getMessage()));
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(chatHistory.getMessageType())) {
                    chatMemory.add(AiMessage.from(chatHistory.getMessage()));
                }
                loadedCount++;
            }
            log.info("为appId：{} 加载 {} 条数据", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载对话记忆失败：" + e.getMessage());
            return 0;
        }
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标，这里的less than是精髓
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

}
