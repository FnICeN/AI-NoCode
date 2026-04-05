package com.nocode.backend.langgraph4j;

import cn.hutool.json.JSONUtil;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.langgraph4j.model.QualityResult;
import com.nocode.backend.langgraph4j.node.*;
import com.nocode.backend.langgraph4j.state.WorkflowContext;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenWorkflow {

    /**
     * 创建完整的工作流
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // 添加节点 - 使用完整实现的节点
                    .addNode("image_collector", ImageCollectorNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // 添加边
                    .addEdge(START, "image_collector")
                    .addEdge("image_collector", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")
                    // 添加条件边，根据路由结果选择执行不同的节点
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",   // 质检通过且需要构建
                                    "skip_build", END,   // 质检通过且不需要构建
                                    "fail", "code_generator"   // 质检不通过，重新生成或修改
                                    ))
                    .addEdge("project_builder", END)

                    // 编译工作流
                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流创建失败");
        }
    }

    /**
     * 执行工作流
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        // 初始化 WorkflowContext
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("初始化")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("工作流图:\n{}", graph.content());
        log.info("开始执行代码生成工作流");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- 第 {} 步完成 ---", stepCounter);
            // 显示当前状态
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("当前步骤上下文: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("代码生成工作流执行完成！");
        return finalContext;
    }

    /**
     * 执行工作流（Flux 流式输出版本）
     */
    public Flux<String> executeWorkflowWithFlux(String originalPrompt) {
        return Flux.create(sink -> {
            // 异步防止请求线程被阻塞
            Thread.startVirtualThread(() -> {
                try {
                    CompiledGraph<MessagesState<String>> workflow = createWorkflow();
                    WorkflowContext initialContext = WorkflowContext.builder()
                            .originalPrompt(originalPrompt)
                            .currentStep("初始化")
                            .build();
                    sink.next(formatSseEvent("workflow_start", Map.of(
                            "message", "开始执行代码生成工作流",
                            "originalPrompt", originalPrompt
                    )));
                    GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
                    log.info("工作流图:\n{}", graph.content());

                    int stepCounter = 1;
                    for (NodeOutput<MessagesState<String>> step : workflow.stream(
                            Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
                        log.info("--- 第 {} 步完成 ---", stepCounter);
                        WorkflowContext currentContext = WorkflowContext.getContext(step.state());
                        if (currentContext != null) {
                            sink.next(formatSseEvent("step_completed", Map.of(
                                    "stepNumber", stepCounter,
                                    "currentStep", currentContext.getCurrentStep()
                            )));
                            log.info("当前步骤上下文: {}", currentContext);
                        }
                        stepCounter++;
                    }
                    sink.next(formatSseEvent("workflow_completed", Map.of(
                            "message", "代码生成工作流执行完成！"
                    )));
                    log.info("代码生成工作流执行完成！");
                    sink.complete();
                } catch (Exception e) {
                    log.error("工作流执行失败: {}", e.getMessage(), e);
                    sink.next(formatSseEvent("workflow_error", Map.of(
                            "error", e.getMessage(),
                            "message", "工作流执行失败"
                    )));
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 格式化 SSE 事件的辅助方法
     */
    private String formatSseEvent(String eventType, Object data) {
        try {
            String jsonData = JSONUtil.toJsonStr(data);
            return "event: " + eventType + "\ndata: " + jsonData + "\n\n";
        } catch (Exception e) {
            log.error("格式化 SSE 事件失败: {}", e.getMessage(), e);
            return "event: error\ndata: {\"error\":\"格式化失败\"}\n\n";
        }
    }


    public String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext workflowContext = WorkflowContext.getContext(state);
        if (workflowContext == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流上下文为空");
        }
        QualityResult qualityResult = workflowContext.getQualityResult();
        CodeGenTypeEnum generationType = workflowContext.getGenerationType();
        // 根据代码检查结果和业务类型决定下一个节点
        if (qualityResult == null || !qualityResult.getIsValid()) {
            return "fail";
        }
        return generationType == CodeGenTypeEnum.VUE_PROJECT ? "build" : "skip_build";
    }

    /**
     * 路由：根据生成类型决定下一步是构建项目还是跳过构建
     */
    private String routeBuildOrSkip(MessagesState<String> state) {
        WorkflowContext workflowContext = WorkflowContext.getContext(state);
        if (workflowContext == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "工作流上下文为空");
        }
        CodeGenTypeEnum generationType = workflowContext.getGenerationType();
        // 根据路由结果决定下一个节点
        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return "build";
        }
        return "skip_build";
    }
}
