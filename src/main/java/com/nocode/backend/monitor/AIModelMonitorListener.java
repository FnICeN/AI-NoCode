package com.nocode.backend.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * AI模型监听器
 */
@Component
public class AIModelMonitorListener implements ChatModelListener {
    private static final String REQUEST_START_TIME_KEY = "requestTime";
    private static final String MONITOR_CONTEXT_KEY = "monitorContext";
    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // 从监控上下文获取信息
        MonitorContext context = MonitorContextHolder.getContext();
        String appId = context.getAppId();
        String userId = context.getUserId();
        // 将主线程的上下文再次保存到请求上下文中，这样后续响应线程才能获取到（因为请求和响应不在同一个线程中）
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);
        // 获取模型名称
        String modelName = requestContext.chatRequest().modelName();
        // 调用指标收集器
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // 从监控上下文中获取信息
        MonitorContext context = MonitorContextHolder.getContext();
        String userId = context.getUserId();
        String appId = context.getAppId();
        // 获取模型名称和错误类型
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        // 记录失败请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        // 记录响应时间（即使是错误响应）
        Map<Object, Object> attributes = errorContext.attributes();
        recordResponseTime(attributes, userId, appId, modelName);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 获得请求响应中传递的监控上下文
        Map<Object, Object> attributes = responseContext.attributes();
        MonitorContext monitorContext = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);
        // 取得监控信息
        String appId = monitorContext.getAppId();
        String userId = monitorContext.getUserId();
        String modelName = responseContext.chatResponse().modelName();
        // 记录
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");
        recordResponseTime(attributes, userId, appId, modelName);
        recordTokenUsage(responseContext, userId, appId, modelName);

    }

    /**
     * 记录响应时间
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        Duration responseTime = Duration.between(startTime, Instant.now());
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
    }

    /**
     * 记录Token使用情况
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
