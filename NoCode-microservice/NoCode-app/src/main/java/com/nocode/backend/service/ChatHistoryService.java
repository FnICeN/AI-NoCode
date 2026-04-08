package com.nocode.backend.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.nocode.backend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.nocode.backend.model.entity.ChatHistory;
import com.nocode.backend.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author FICN
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存用户消息
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @param message 消息内容
     * @return 对话历史ID
     */
    boolean saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存AI消息
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @param message 消息内容
     * @return 对话历史ID
     */
    boolean saveAIMessage(Long appId, Long userId, String message);

    /**
     * 保存错误消息
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @param errorMessage 错误消息内容
     * @return 对话历史ID
     */
    boolean saveErrorMessage(Long appId, Long userId, String errorMessage);

    /**
     * 分页查询应用的对话历史
     *
     * @param appId 应用ID
     * @param pageSize 每页大小
     * @param lastCreateTime 最近创建时间
     * @param loginUser 当前登录用户
     * @return 分页对象
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 管理员查询所有对话历史
     *
     * @param pageSize 每页大小
     * @param offset 偏移量
     * @return 分页对象
     */
    Page<ChatHistory> listAllChatHistory(int pageSize, long offset);

    /**
     * 根据应用ID删除对话历史
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    boolean deleteChatHistoryByAppId(Long appId);

    /**
     * 验证用户是否有权限查看应用的对话历史
     *
     * @param appId 应用ID
     * @param loginUser 当前登录用户
     * @return 是否有权限
     */
    boolean validatePermission(Long appId, User loginUser);

    /**
     * 保存消息的通用方法
     *
     * @param appId      应用ID
     * @param userId     用户ID
     * @param message    消息内容
     * @param messageType 消息类型
     * @return 保存是否成功
     */
    boolean saveMessage(Long appId, Long userId, String message, String messageType);

    /**
     * 将数据库的对话历史加载到 ChatMemory
     *
     * @param appId 应用ID
     * @param chatMemory 对话记忆对象
     * @param maxCount 最大加载条数
     * @return 成功加载条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 构造游标查询请求
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
