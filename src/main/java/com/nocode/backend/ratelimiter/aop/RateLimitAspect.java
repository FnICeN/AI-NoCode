package com.nocode.backend.ratelimiter.aop;

import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.entity.User;
import com.nocode.backend.ratelimiter.annotation.RateLimit;
import com.nocode.backend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint jp, RateLimit rateLimit) {
        String key = generateRateLimitKey(jp, rateLimit);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofHours(1));   // 1小时后过期
        // 设置限流器参数
        rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS);
        // 尝试获取一个令牌，失败则限流
        if (!rateLimiter.tryAcquire(1)) {
            log.error("令牌不足，触发Redisson限流");
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    }

    private String generateRateLimitKey(JoinPoint jp, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // 根据限流类型生成不同的key
        switch (rateLimit.limitType()) {
            case API:
                // 对于接口，使用方法名作为key，保证多次调用使用同一个key，拿到同一个限流器
                MethodSignature signature = (MethodSignature) jp.getSignature();
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".").append(method.getName());
                break;
            case USER:
                // 对于用户，使用用户ID作为key，保证相同用户使用相同的key
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request);
                        keyBuilder.append("user:").append(loginUser.getId());
                    } else {
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    // 未登录用户使用IP限流
                    keyBuilder.append("ip:").append(getClientIP());
                }
                break;
            case IP:
                // 对于IP，使用IP作为key，保证相同IP使用相同的key
                keyBuilder.append("ip:").append(getClientIP());
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }
    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return "unknown";
        String ip = null;
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        return ip != null ? ip : "unknown";
    }
}
