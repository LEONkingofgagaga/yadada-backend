package com.yage.yadada.model.dto.question;

import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsProject: yadada-backend
 * @BelongsPackage: com.yage.yadada.model.dto.question
 * @Author: liyajun
 * @CreateTime: 2025-02-28  20:49
 * @Description: AI生成题目请求
 * @Version: 1.0
 */
@Data
public class AiGenerateQuestionRequest implements Serializable {

    /**
     * 应用id
     *  */
    private Long appId;

    /**
    *  题目数
    * */
    int questionNumber = 10;

    /**
    * 选项数
    * */
    int optionNumber = 2;
}
