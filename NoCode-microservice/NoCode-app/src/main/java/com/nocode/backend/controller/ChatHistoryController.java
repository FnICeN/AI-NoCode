package com.nocode.backend.controller;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.nocode.backend.annotation.AuthCheck;
import com.nocode.backend.common.BaseResponse;
import com.nocode.backend.common.DeleteRequest;
import com.nocode.backend.common.ResultUtils;
import com.nocode.backend.constant.UserConstant;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.exception.ThrowUtils;
import com.nocode.backend.innerservice.InnerUserService;
import com.nocode.backend.model.dto.chathistory.ChatHistoryQueryRequest;
import com.nocode.backend.model.entity.ChatHistory;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.service.ChatHistoryService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author FICN
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    @Lazy
    private InnerUserService userService;

    // ==================== 用户接口 ====================

    /**
     * 保存用户消息
     *
     * @param appId    应用ID
     * @param message  消息内容
     * @param request  请求对象
     * @return 对话历史ID
     */
    @PostMapping("/save/userMessage")
    public BaseResponse<Boolean> saveUserMessage(@RequestParam Long appId, @RequestParam String message, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        User loginUser = InnerUserService.getLoginUser(request);
        boolean result = chatHistoryService.saveUserMessage(appId, loginUser.getId(), message);
        return ResultUtils.success(result);
    }

    /**
     * 保存AI消息
     *
     * @param appId    应用ID
     * @param message  消息内容
     * @param request  请求对象
     * @return 对话历史ID
     */
    @PostMapping("/save/aiMessage")
    public BaseResponse<Boolean> saveAIMessage(@RequestParam Long appId, @RequestParam String message, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(message == null || message.isEmpty(), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        User loginUser = InnerUserService.getLoginUser(request);
        boolean result = chatHistoryService.saveAIMessage(appId, loginUser.getId(), message);
        return ResultUtils.success(result);
    }

    /**
     * 保存错误消息
     *
     * @param appId         应用ID
     * @param errorMessage  错误消息内容
     * @param request       请求对象
     * @return 对话历史ID
     */
    @PostMapping("/save/errorMessage")
    public BaseResponse<Boolean> saveErrorMessage(@RequestParam Long appId, @RequestParam String errorMessage, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(errorMessage == null || errorMessage.isEmpty(), ErrorCode.PARAMS_ERROR, "错误消息内容不能为空");
        User loginUser = InnerUserService.getLoginUser(request);
        boolean result = chatHistoryService.saveErrorMessage(appId, loginUser.getId(), errorMessage);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询应用的对话历史
     *
     * @param appId     应用ID
     * @param pageSize  每页大小
     * @param lastCreateTime 最近创建时间
     * @param request   请求对象
     * @return 分页对象
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listChatHistoryByAppId(@PathVariable Long appId, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false)LocalDateTime lastCreateTime, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页大小错误");
        User loginUser = InnerUserService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员查询所有对话历史
     * @param chatHistoryQueryRequest 对话请求对象
     * @param request 请求对象
     * @return 分页对话历史
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistory(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        int pageNum = chatHistoryQueryRequest.getPageNum();
        int pageSize = chatHistoryQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "每页大小错误");
        // 管理员既可能按id查询，又可能查询全部，所以不用封装好的listAppChatHistoryByPage方法，而是现场组装一个命令
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
        return ResultUtils.success(result);
    }

    /**
     * 根据应用ID删除对话历史
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/admin/deleteByAppId")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistoryByAppId(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean result = chatHistoryService.deleteChatHistoryByAppId(deleteRequest.getId());
        return ResultUtils.success(result);
    }

}
