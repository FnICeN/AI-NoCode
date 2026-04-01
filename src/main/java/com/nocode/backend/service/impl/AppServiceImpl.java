package com.nocode.backend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nocode.backend.constant.AppConstant;
import com.nocode.backend.core.AICodeGeneratorFacade;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.exception.ThrowUtils;
import com.nocode.backend.mapper.AppMapper;
import com.nocode.backend.model.dto.app.*;
import com.nocode.backend.model.entity.App;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.enums.ChatHistoryMessageTypeEnum;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import com.nocode.backend.model.vo.AppVO;
import com.nocode.backend.model.vo.UserVO;
import com.nocode.backend.service.AppService;
import com.nocode.backend.service.ChatHistoryService;
import com.nocode.backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author FICN
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private AICodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public long addApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String initPrompt = appAddRequest.getInitPrompt();
        String appName = initPrompt.substring(0, Math.min(initPrompt.length(), 12));
        if (StrUtil.hasBlank(appName, initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称或初始化提示词为空");
        }

        // 创建应用
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setAppName(appName);
        app.setUserId(loginUser.getId());
        // TODO: 创建应用时默认设置为多文件生成应用，之后可能要改
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        app.setPriority(0);
        app.setEditTime(LocalDateTime.now());

        boolean saveResult = this.save(app);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建应用失败");
        }
        return app.getId();
    }

    @Override
    public boolean deleteApp(Long id, User loginUser) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }

        // 查询应用
        App app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 校验权限（只能删除自己的应用）
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权删除他人应用");
        }

        return this.removeById(id);
    }

    @Override
    public boolean updateApp(AppUpdateRequest appUpdateRequest, User loginUser) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appUpdateRequest.getId();
        String appName = appUpdateRequest.getAppName();

        if (StrUtil.isBlank(appName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称为空");
        }
        if (appName.length() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称过长");
        }

        // 查询应用
        App oldApp = this.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 校验权限（只能更新自己的应用）
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权更新他人应用");
        }

        // 更新应用
        App app = new App();
        app.setId(id);
        app.setAppName(appName);
        app.setEditTime(LocalDateTime.now());

        return this.updateById(app);
    }

    @Override
    public App getAppById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }
        App app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        return app;
    }

    // 只查询自己的，所以传入的appQueryRequest中可以没有userId（实际上也没定义）
    @Override
    public Page<AppVO> listMyApps(AppQueryRequest appQueryRequest, User loginUser) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 分页参数
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = Math.min(appQueryRequest.getPageSize(), 20);

        // 构建查询条件
        QueryWrapper queryWrapper = getQueryWrapper(appQueryRequest, loginUser.getId());

        // 执行查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    @Override
    public Page<AppVO> listFeaturedApps(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 分页参数
        int pageNum = appQueryRequest.getPageNum();
        int pageSize = Math.min(appQueryRequest.getPageSize(), 20);

        // 构建查询条件（精选应用：优先级大于0，按优先级降序）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("priority", AppConstant.GOOD_APP_PRIORITY);

        // 应用名称模糊查询
        if (StrUtil.isNotBlank(appQueryRequest.getAppName())) {
            queryWrapper.like("appName", appQueryRequest.getAppName());
        }

        queryWrapper.orderBy("priority", false);

        // 执行查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    @Override
    public boolean deleteAppByAdmin(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用id不合法");
        }
        App app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        return this.removeById(id);
    }

    @Override
    public boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appAdminUpdateRequest.getId();

        // 查询应用是否存在
        App oldApp = this.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 更新应用
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());

        return this.updateById(app);
    }

    @Override
    public Page<AppVO> listAppsByAdmin(AppAdminQueryRequest appAdminQueryRequest) {
        if (appAdminQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        // 分页参数（管理员查询不限每页数量）
        int pageNum = appAdminQueryRequest.getPageNum();
        int pageSize = appAdminQueryRequest.getPageSize();

        // 构建查询条件
        QueryWrapper queryWrapper = getAdminQueryWrapper(appAdminQueryRequest);

        // 执行查询
        Page<App> page = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为VO
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, page.getTotalRow());
        List<AppVO> appVOList = getAppVOList(page.getRecords());
        appVOPage.setRecords(appVOList);

        return appVOPage;
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户对象
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> apps) {
        if (CollUtil.isEmpty(apps)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = apps.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return apps.stream()
                .map(app -> {
                    AppVO appVO = getAppVO(app);
                    UserVO userVO = userVOMap.get(app.getUserId());
                    appVO.setUser(userVO);
                    return appVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long userId) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求参数为空");
        }
        String appName = appQueryRequest.getAppName();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId);

        // 应用名称模糊查询
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like("appName", appName);
        }

        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }

        return queryWrapper;
    }

    // 管理员查询业务。本质上也可以用来给用户查，鱼皮视频中就是将这个业务提供给用户了，只不过写死了只传入userId
    @Override
    public QueryWrapper getAdminQueryWrapper(AppAdminQueryRequest appAdminQueryRequest) {
        if (appAdminQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求参数为空");
        }
        Long id = appAdminQueryRequest.getId();
        String appName = appAdminQueryRequest.getAppName();
        String cover = appAdminQueryRequest.getCover();
        String codeGenType = appAdminQueryRequest.getCodeGenType();
        String deployKey = appAdminQueryRequest.getDeployKey();
        Integer priority = appAdminQueryRequest.getPriority();
        Long userId = appAdminQueryRequest.getUserId();
        String sortField = appAdminQueryRequest.getSortField();
        String sortOrder = appAdminQueryRequest.getSortOrder();

        QueryWrapper queryWrapper = QueryWrapper.create();
        
        // 等值查询
        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (userId != null) {
            queryWrapper.eq("userId", userId);
        }
        if (priority != null) {
            queryWrapper.eq("priority", priority);
        }
        if (StrUtil.isNotBlank(codeGenType)) {
            queryWrapper.eq("codeGenType", codeGenType);
        }
        if (StrUtil.isNotBlank(deployKey)) {
            queryWrapper.eq("deployKey", deployKey);
        }
        
        // 模糊查询
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like("appName", appName);
        }
        if (StrUtil.isNotBlank(cover)) {
            queryWrapper.like("cover", cover);
        }
        
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }
        
        return queryWrapper;
    }

    /**
     *
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 当前用户对象
     * @return
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");

        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "生成类型错误");

        // 保存User历史记录
        chatHistoryService.saveUserMessage(appId, loginUser.getId(), message);
        // 获取AI回复流
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId);
        // 收集AI响应内容，保存到数据库
        StringBuilder aiResponseBuilder = new StringBuilder();
        return contentFlux.map(chunk -> {
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

    @Override
    public String deployApp(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID错误");

        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");

        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey))
            deployKey = RandomUtil.randomString(6);

        // 获取应用浏览目录
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;

        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists(), ErrorCode.SYSTEM_ERROR, "代码生成路径不存在");

        // 将文件 从浏览目录复制到部署目录（tmp/code_deploy/{deployKey}）
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署（复制）失败" + e.getMessage());
        }

        App updateApp = new App();
        // 这里将id填入是为了让updateById可以找到该app
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");

        return String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    /**
     * 删除应用时顺便删除对话历史
     *
     * @param appId 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable appId) {
        if (appId == null) {
            return false;
        }
        long id = Long.parseLong(appId.toString());
        if (id <= 0) {
            return false;
        }
        try {
            chatHistoryService.deleteChatHistoryByAppId(id);
        } catch (Exception e) {
            log.error("删除应用对应对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(appId);
    }

}
