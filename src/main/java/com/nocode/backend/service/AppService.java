package com.nocode.backend.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.nocode.backend.model.dto.app.*;
import com.nocode.backend.model.entity.App;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author FICN
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param loginUser     当前用户对象
     * @return 新应用id
     */
    long addApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 删除应用（用户只能删除自己的应用）
     *
     * @param id      应用id
     * @param loginUser 当前用户对象
     * @return 是否成功
     */
    boolean deleteApp(Long id, User loginUser);

    /**
     * 更新应用（用户只能更新自己的应用）
     *
     * @param appUpdateRequest 更新请求
     * @param loginUser 当前用户对象
     * @return 是否成功
     */
    boolean updateApp(AppUpdateRequest appUpdateRequest, User loginUser);

    /**
     * 根据id获取应用详情
     *
     * @param id 应用id
     * @return 应用实体
     */
    App getAppById(Long id);

    /**
     * 分页查询自己的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param loginUser 当前用户对象
     * @return 分页对象
     */
    Page<AppVO> listMyApps(AppQueryRequest appQueryRequest, User loginUser);

    /**
     * 分页查询精选应用列表
     *
     * @param appQueryRequest 查询请求
     * @return 分页对象
     */
    Page<AppVO> listFeaturedApps(AppQueryRequest appQueryRequest);

    /**
     * 管理员删除任意应用
     *
     * @param id 应用id
     * @return 是否成功
     */
    boolean deleteAppByAdmin(Long id);

    /**
     * 管理员更新任意应用
     *
     * @param appAdminUpdateRequest 更新请求
     * @return 是否成功
     */
    boolean updateAppByAdmin(AppAdminUpdateRequest appAdminUpdateRequest);

    /**
     * 管理员分页查询应用列表
     *
     * @param appAdminQueryRequest 查询请求
     * @return 分页对象
     */
    Page<AppVO> listAppsByAdmin(AppAdminQueryRequest appAdminQueryRequest);

    /**
     * 获取脱敏后的应用信息
     *
     * @param app 应用实体
     * @return 应用VO
     */
    AppVO getAppVO(App app);

    /**
     * 获取脱敏后的应用信息列表
     *
     * @param apps 应用实体列表
     * @return 应用VO列表
     */
    List<AppVO> getAppVOList(List<App> apps);

    /**
     * 将用户查询请求转换为QueryWrapper对象
     *
     * @param appQueryRequest 查询请求对象
     * @param userId          用户id
     * @return QueryWrapper对象
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest, Long userId);

    /**
     * 将管理员查询请求转换为QueryWrapper对象
     *
     * @param appAdminQueryRequest 查询请求对象
     * @return QueryWrapper对象
     */
    QueryWrapper getAdminQueryWrapper(AppAdminQueryRequest appAdminQueryRequest);

    /**
     * 通过对话生成应用代码
     *
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 当前用户对象
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

}
