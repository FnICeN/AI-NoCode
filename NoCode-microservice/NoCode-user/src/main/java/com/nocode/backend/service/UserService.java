package com.nocode.backend.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.nocode.backend.model.dto.UserQueryRequest;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.vo.LoginUserVO;
import com.nocode.backend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author FICN
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @return 登录用户
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏用户信息
     *
     * @param user 用户实体
     * @return 脱敏用户实体
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取登录用户信息
     *
     * @param request 请求对象
     * @return 用户实体
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取其他用户看到的脱敏用户信息（单个）
     *
     * @param user 用户实体
     * @return 用户看到的他人脱敏用户实体
     */
    UserVO getUserVO(User user);

    /**
     * 获取其他用户看到的脱敏用户信息（多个分页）
     *
     * @param users 多个用户实体
     * @return 用户看到的他人脱敏用户实体（多个）
     */
    List<UserVO> getUserVOList(List<User> users);

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return 是否成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 对密码取摘要
     *
     * @param password 密码原文
     * @return 摘要
     */
    String getEncryptPassword(String password);

    /**
     * 将查询请求转换为QueryWrapper对象用于查询
     *
     * @param request 查询请求对象
     * @return QueryWrapper对象
     */
    QueryWrapper getQueryWrapper(UserQueryRequest request);

}
