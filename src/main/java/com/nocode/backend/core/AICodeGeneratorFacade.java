package com.nocode.backend.core;

import cn.hutool.json.JSONUtil;
import com.nocode.backend.ai.AICodeGeneratorService;
import com.nocode.backend.ai.AICodeGeneratorServiceFactory;
import com.nocode.backend.ai.model.HtmlCodeResult;
import com.nocode.backend.ai.model.MultiFileCodeResult;
import com.nocode.backend.ai.model.message.AIResponseMessage;
import com.nocode.backend.ai.model.message.ToolExecutedMessage;
import com.nocode.backend.ai.model.message.ToolRequestMessage;
import com.nocode.backend.constant.AppConstant;
import com.nocode.backend.core.builder.VueProjectBuilder;
import com.nocode.backend.core.parser.CodeParserExecutor;
import com.nocode.backend.core.saver.CodeFileSaverExecutor;
import com.nocode.backend.exception.BusinessException;
import com.nocode.backend.exception.ErrorCode;
import com.nocode.backend.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，组合代码生成和保存功能
 */
@Slf4j
@Service
public class AICodeGeneratorFacade {
    @Resource
    private AICodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 根据类型生成并保存代码
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        // 根据appId获取定制AI Service
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId, codeGenType);
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    /**
     * 根据类型生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @param appId 应用ID
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        if (codeGenType == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不可为空");
        // 这里获得的Service根据传入的codeGenType决定，可能是chat也可能是reason
        // 获取Service时统一调用双参数方法，如果是VUE则正好获得reason模型，如果不是则获得chat模型，正好可以给HTML和MULTI_FILE用
        AICodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAICodeGeneratorService(appId, codeGenType);
        return switch (codeGenType) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                // 获取的是 reason 模型进行 Vue 项目生成
                TokenStream result = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                // 转换为 Flux<String>，返回的是流式的一个个JSON对象，而非破碎零件，因此下游需要单独处理，与HTML或MULTI_FILE不同
                yield processTokenStream(result, appId);
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型错误");
        };
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @param appId 应用ID，用于打包
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        // 这个process可以不用管保存了，因为AI已经使用保存工具保存过了，因此省去了一次chunk拼接，只需要在下游保存到数据库即可
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                // 将 partialResponse 转换为 AIResponseMessage，然后转为JSON传给下游，因为下游会解析JSON
                        AIResponseMessage aiResponseMessage = new AIResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onCompleteResponse((ChatResponse response) -> {
                        // 同步打包VUE项目
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProjectAsync(projectPath);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 通用代码生成与保存（流式）
     * @param codeStream 代码流
     * @param codeGenType 代码类型
     * @param appId 应用ID
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 定义字符串拼接器，拼接所有流式返回用于之后解析保存
        StringBuilder sb = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            sb.append(chunk);
        }).doOnComplete(() -> {
            try {
                Object parsedResult = CodeParserExecutor.executeParser(sb.toString(), codeGenType);
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("文件保存完成，目录：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("文件保存失败，{}", e.getMessage() );
            }
        });
    }
}
