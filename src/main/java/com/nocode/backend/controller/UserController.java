package com.nocode.backend.controller;

import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.audit.ConsoleMessageCollector;
import com.mybatisflex.core.audit.MessageCollector;
import com.mybatisflex.core.paginate.Page;
import com.nocode.backend.annotation.AuthCheck;
import com.nocode.backend.common.BaseResponse;
import com.nocode.backend.common.DeleteRequest;
import com.nocode.backend.common.PageRequest;
import com.nocode.backend.common.ResultUtils;
import com.nocode.backend.constant.UserConstant;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.exception.ThrowUtils;
import com.nocode.backend.model.dto.*;
import com.nocode.backend.model.enums.UserRoleEnum;
import com.nocode.backend.model.vo.LoginUserVO;
import com.nocode.backend.model.vo.UserVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.service.UserService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import static com.nocode.backend.constant.UserConstant.DEFAULT_PASSWORD;

/**
 * 用户 控制层。
 *
 * @author FICN
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 注册请求
     * @return 成功响应
     */
    @PostMapping("register")
    public BaseResponse<Long> register(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 登录请求
     * @param httpServletRequest 请求对象
     * @return 脱敏后的登录信息
     */
    @PostMapping("login")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, httpServletRequest);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取登录用户信息
     *
     * @param httpServletRequest 请求对象
     * @return 脱敏后用户信息
     */
    @GetMapping("get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     *
     * @param httpServletRequest 请求对象
     * @return 是否成功
     */
    @PostMapping("logout")
    public BaseResponse<Boolean> logout(HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(httpServletRequest == null, ErrorCode.PARAMS_ERROR);
        boolean logoutResult = userService.userLogout(httpServletRequest);
        return ResultUtils.success(logoutResult);
    }

    /**
     * 创建用户
     *
     * @param userAddRequest 创建用户请求
     * @return 新建用户Id
     */
    @PostMapping("add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        user.setUserPassword(userService.getEncryptPassword(DEFAULT_PASSWORD));
        boolean saveResult = userService.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 管理员根据Id获取用户信息
     * @param id
     * @return
     */
    @GetMapping("get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    @GetMapping("get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        User user = getUserById(id).getData();
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);
    }

    /**
     * 删除用户。
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean deleteResult = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(deleteResult);
    }

    /**
     * 更新用户。
     *
     * @param userUpdateRequest 更新请求
     * @return 是否成功
     */
    @PostMapping("update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean updateResult = userService.updateById(user);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询脱敏后他人信息
     *
     * @param userQueryRequest 查询请求
     * @return 分页对象
     */
    @PostMapping("list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
