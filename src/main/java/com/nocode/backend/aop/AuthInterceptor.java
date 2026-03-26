package com.nocode.backend.aop;

import com.nocode.backend.annotation.AuthCheck;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.model.enums.UserRoleEnum;
import com.nocode.backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param pjp 切入点
     * @param authCheck 权限校验注解
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doIntercept(ProceedingJoinPoint pjp, AuthCheck authCheck) throws Throwable {
        // 得到的是"admin"或"user"，而非枚举类
        String mustRole = authCheck.mustRole();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取用户角色枚举类与必须角色枚举类
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        if (mustRoleEnum == null) {
            return pjp.proceed();
        }
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求有ADMIN但实际用户不是
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return pjp.proceed();
    }
}
