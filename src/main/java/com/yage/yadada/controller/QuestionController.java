package com.yage.yadada.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yage.yadada.annotation.AuthCheck;
import com.yage.yadada.common.BaseResponse;
import com.yage.yadada.common.DeleteRequest;
import com.yage.yadada.common.ErrorCode;
import com.yage.yadada.common.ResultUtils;
import com.yage.yadada.constant.UserConstant;
import com.yage.yadada.exception.BusinessException;
import com.yage.yadada.exception.ThrowUtils;
import com.yage.yadada.manager.AiManager;
import com.yage.yadada.model.dto.question.*;
import com.yage.yadada.model.entity.App;
import com.yage.yadada.model.entity.Question;
import com.yage.yadada.model.entity.User;
import com.yage.yadada.model.enums.AppTypeEnum;
import com.yage.yadada.model.vo.QuestionVO;
import com.yage.yadada.service.AppService;
import com.yage.yadada.service.QuestionService;
import com.yage.yadada.service.UserService;
import com.zhipu.oapi.service.v4.model.ModelData;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题目表接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    @Resource
    private AppService appService;

    @Resource
    private AiManager aiManager;


    @Resource
    private Scheduler vipScheduler;
    // region 增删改查

    /**
     * 创建题目表
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionAddRequest == null, ErrorCode.PARAMS_ERROR);
        //在此处将实体类QuestionAddRequest和 DTO进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<QuestionContentDTO> questionContentDTO = questionAddRequest.getQuestionContent();
        question.setQuestionContent(JSONUtil.toJsonStr(questionContentDTO));
        // 数据校验
        questionService.validQuestion(question, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除题目表
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题目表（仅管理员可用）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        // 数据校验
        questionService.validQuestion(question, false);
        // 判断是否存在
        long id = questionUpdateRequest.getId();
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionService.updateById(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题目表（封装类）
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Question question = questionService.getById(id);
        ThrowUtils.throwIf(question == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVO(question, request));
    }

    /**
     * 分页获取题目表列表（仅管理员可用）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Question>> listQuestionByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionPage);
    }

    /**
     * 分页获取题目表列表（封装类）
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 分页获取当前登录用户创建的题目表列表
     *
     * @param questionQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionVO>> listMyQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionQueryRequest.setUserId(loginUser.getId());
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionService.getQuestionVOPage(questionPage, request));
    }

    /**
     * 编辑题目表（给用户使用）
     *
     * @param questionEditRequest
     * @param request
     * @return
     */
//    @PostMapping("/edit")
//    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
//        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        // todo 在此处将实体类和 DTO 进行转换
//        Question question = new Question();
//        BeanUtils.copyProperties(questionEditRequest, question);
//        // 数据校验
//        questionService.validQuestion(question, false);
//        User loginUser = userService.getLoginUser(request);
//        // 判断是否存在
//        long id = questionEditRequest.getId();
//        Question oldQuestion = questionService.getById(id);
//        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可编辑
//        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // 操作数据库
//        boolean result = questionService.updateById(question);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }
    @PostMapping("/edit")
    public BaseResponse<Boolean> editQuestion(@RequestBody QuestionEditRequest questionEditRequest, HttpServletRequest request) {
        if (questionEditRequest == null || questionEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 2. 查询待更新的原数据
        //获取题目id
        Long id = questionEditRequest.getId();
        //根据id查询出原数据
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);

        // 3. 权限校验：仅本人或管理员可编辑
        if (!oldQuestion.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 4. 将 DTO 转为 Entity，并保留未修改的字段值
        Question question = new Question();
        BeanUtils.copyProperties(questionEditRequest, question);

        // 关键修复：确保至少有一个字段需要更新
        if (question.getQuestionContent() == null) {
            question.setQuestionContent(oldQuestion.getQuestionContent());
        }
        if (question.getAppId() == null) {
            question.setAppId(oldQuestion.getAppId());
        }

        // 5. 数据合法性校验
        questionService.validQuestion(question, false);

        // 6. 使用 UpdateWrapper 动态构建 SQL，避免空 SET 子句
        UpdateWrapper<Question> updateWrapper = new UpdateWrapper<>();
        updateWrapper
                .eq("id", id)
                .eq("isDelete", 0) // 逻辑删除条件
                .set(question.getQuestionContent() != null, "questionContent", question.getQuestionContent())
                .set(question.getAppId() != null, "appId", question.getAppId());

        // 7. 执行更新
        boolean result = questionService.update(updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }


    // endregion

    // region AI 生成题目功能

    // 系统引导提示词
    private static final String GENERATE_QUESTION_SYSTEM_MESSAGE = "你是一位严谨的出题专家，我会给你如下信息：\n" +
            "```\n" +
            "应用名称，\n" +
            "【【【应用描述】】】，\n" +
            "应用类别，\n" +
            "要生成的题目数，\n" +
            "每个题目的选项数\n" +
            "```\n" +
            "\n" +
            "请你根据上述信息，按照以下步骤来出题：\n" +
            "1. 要求：题目和选项尽可能地短，题目不要包含序号，每题的选项数以我提供的为主，题目不能重复\n" +
            "2. 严格按照下面的 json 格式输出题目和选项\n" +
            "```\n" +
            "[{\"options\":[{\"value\":\"选项内容\",\"key\":\"A\"},{\"value\":\"\",\"key\":\"B\"}],\"title\":\"题目标题\"}]\n" +
            "```\n" +
            "title 是题目，options 是选项，每个选项的 key 按照英文字母序（比如 A、B、C、D）以此类推，value 是选项内容\n" +
            "3. 检查题目是否包含序号，若包含序号则去除序号\n" +
            "4. 返回的题目列表格式必须为 JSON 数组";

    // 用户引导提示词，接受参数使用函数生成

    /**
     * 生成题目的用户消息
     * MBTI 性格测试，
     * 【【【快来测测你的 MBTI 性格】】】，
     * 测评类，
     * 10，
     * 3
     *
     * @param app
     * @param questionNumber
     * @param optionNumber
     * @return
     */
    private String getGenerateQuestionUserMessage(App app, int questionNumber, int optionNumber) {
        StringBuilder userMessage = new StringBuilder();
        userMessage.append(app.getAppName()).append("\n");
        userMessage.append(app.getAppDesc()).append("\n");
        userMessage.append(AppTypeEnum.getEnumByValue(app.getAppType()).getText() + "类").append("\n");
        userMessage.append(questionNumber).append("\n");
        userMessage.append(optionNumber);
        return userMessage.toString();
    }

    //    @PostMapping("/ai_generate")
//    public BaseResponse<List<QuestionContentDTO>> aiGenerateQuestion(@RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest) {
//       ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
//
//        // 获取参数
//        Long appId = aiGenerateQuestionRequest.getAppId();
//        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
//        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
//
//        // 获取应用信息
//        App app = appService.getById(appId);
//        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
//
//        // 封装 Prompt
//        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
//
//        // Ai 生成
//        String result = aiManager.doSyncStableRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage);
//
//        int start = result.indexOf("[");
//        int end = result.lastIndexOf("]");
//        String json = result.substring(start, end + 1);
//        System.out.println(json);
//        List<QuestionContentDTO> questionContentOTOList = JSONUtil.toList(json, QuestionContentDTO.class);
//        return ResultUtils.success(questionContentOTOList);
//    }
    @Autowired
    private ObjectMapper objectMapper; // Spring Boot 自动配置的实例

    @PostMapping("/ai_generate")
    public BaseResponse<List<QuestionContentDTO>> aiGenerateQuestion(
            @RequestBody AiGenerateQuestionRequest aiGenerateQuestionRequest) throws JsonProcessingException {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // AI 生成
        String result = aiManager.doSyncRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);

        //截取需要的 JSON 信息,否者会报错
        JSONObject resultJson = JSONUtil.parseObj(result);
        result = (String) ((JSONObject) resultJson.get("message")).get("content");
        System.out.println(result);

        int start = result.indexOf("[");
        int end = result.lastIndexOf("]");

        String json = result.substring(start, end + 1);
        System.out.println(json);

        // 校验并解析
        if (!JSONUtil.isJsonArray(json)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 返回 JSON 格式错误");
        }

        List<QuestionContentDTO> questionContentDTOList = JSONUtil.toList(json, QuestionContentDTO.class);
        return ResultUtils.success(questionContentDTOList);
    }
    // endregion

//    @GetMapping("/ai_generate/sse")
//    public SseEmitter aiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest) {
//        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
//        // 获取参数
//        Long appId = aiGenerateQuestionRequest.getAppId();
//        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
//        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
//        // 获取应用信息
//        App app = appService.getById(appId);
//        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
//        // 封装 Prompt
//        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
//        // 建立 SSE 连接对象，0 表示永不超时
//        SseEmitter sseEmitter = new SseEmitter(0L);
//        // AI 生成，SSE 流式返回
//        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
//        // 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
//        AtomicInteger counter = new AtomicInteger(0);
//        // 拼接完整题目
//        StringBuilder stringBuilder = new StringBuilder();
//        modelDataFlowable
//                .observeOn(Schedulers.io())
//                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
//                .map(message -> message.replaceAll("\\s", ""))
//                .filter(StrUtil::isNotBlank)
//                .flatMap(message -> {
//                    List<Character> characterList = new ArrayList<>();
//                    for (char c : message.toCharArray()) {
//                        characterList.add(c);
//                    }
//                    return Flowable.fromIterable(characterList);
//                })
//                .doOnNext(c -> {
//                    // 如果是 '{'，计数器 + 1
//                    if (c == '{') {
//                        counter.addAndGet(1);
//                    }
//                    if (counter.get() > 0) {
//                        stringBuilder.append(c);
//                    }
//                    if (c == '}') {
//                        counter.addAndGet(-1);
//                        if (counter.get() == 0) {
//                            // 可以拼接题目，并且通过 SSE 返回给前端
//                            sseEmitter.send(JSONUtil.toJsonStr(stringBuilder.toString()));
//                            // 重置，准备拼接下一道题
//                            stringBuilder.setLength(0);
//                        }
//                    }
//                })
//                .doOnError((e) -> log.error("sse error", e))
//                .doOnComplete(sseEmitter::complete)
//                .subscribe();
//        return sseEmitter;
//    }

    @GetMapping("/ai_generate/sse")
    public SseEmitter aiGenerateQuestionSSE(AiGenerateQuestionRequest aiGenerateQuestionRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // 建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        // AI 生成，SSE 流式返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        // 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        // 拼接完整题目
        StringBuilder stringBuilder = new StringBuilder();

        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        // 默认全局线程池
        Scheduler scheduler = Schedulers.io();
        if ("vip".equals(loginUser.getUserRole())) {
            scheduler = vipScheduler;
        }
        modelDataFlowable
                .observeOn(scheduler)
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果是 '{'，计数器 + 1
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        stringBuilder.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 可以拼接题目，并且通过 SSE 返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(stringBuilder.toString()));
                            // 重置，准备拼接下一道题
                            stringBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("sse error", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();
        return sseEmitter;
    }

    // 仅测试隔离线程池使用
    @Deprecated
    @GetMapping("/ai_generate/sse/test")
    public SseEmitter aiGenerateQuestionSSETest(AiGenerateQuestionRequest aiGenerateQuestionRequest,
                                                boolean isVip) {
        ThrowUtils.throwIf(aiGenerateQuestionRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long appId = aiGenerateQuestionRequest.getAppId();
        int questionNumber = aiGenerateQuestionRequest.getQuestionNumber();
        int optionNumber = aiGenerateQuestionRequest.getOptionNumber();
        // 获取应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 封装 Prompt
        String userMessage = getGenerateQuestionUserMessage(app, questionNumber, optionNumber);
        // 建立 SSE 连接对象，0 表示永不超时
        SseEmitter sseEmitter = new SseEmitter(0L);
        // AI 生成，SSE 流式返回
        Flowable<ModelData> modelDataFlowable = aiManager.doStreamRequest(GENERATE_QUESTION_SYSTEM_MESSAGE, userMessage, null);
        // 左括号计数器，除了默认值外，当回归为 0 时，表示左括号等于右括号，可以截取
        AtomicInteger counter = new AtomicInteger(0);
        // 拼接完整题目
        StringBuilder stringBuilder = new StringBuilder();
        // 默认全局线程池
        Scheduler scheduler = Schedulers.single();
        if (isVip) {
            scheduler = vipScheduler;
        }
        modelDataFlowable
                .observeOn(scheduler)
                .map(modelData -> modelData.getChoices().get(0).getDelta().getContent())
                .map(message -> message.replaceAll("\\s", ""))
                .filter(StrUtil::isNotBlank)
                .flatMap(message -> {
                    List<Character> characterList = new ArrayList<>();
                    for (char c : message.toCharArray()) {
                        characterList.add(c);
                    }
                    return Flowable.fromIterable(characterList);
                })
                .doOnNext(c -> {
                    // 如果是 '{'，计数器 + 1
                    if (c == '{') {
                        counter.addAndGet(1);
                    }
                    if (counter.get() > 0) {
                        stringBuilder.append(c);
                    }
                    if (c == '}') {
                        counter.addAndGet(-1);
                        if (counter.get() == 0) {
                            // 输出当前线程的名称
                            System.out.println(Thread.currentThread().getName());
                            // 模拟普通用户阻塞
                            if (!isVip) {
                                Thread.sleep(10000L);
                            }
                            // 可以拼接题目，并且通过 SSE 返回给前端
                            sseEmitter.send(JSONUtil.toJsonStr(stringBuilder.toString()));
                            // 重置，准备拼接下一道题
                            stringBuilder.setLength(0);
                        }
                    }
                })
                .doOnError((e) -> log.error("sse error", e))
                .doOnComplete(sseEmitter::complete)
                .subscribe();
        return sseEmitter;
    }

}
