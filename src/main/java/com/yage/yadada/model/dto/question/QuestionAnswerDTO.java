package com.yage.yadada.model.dto.question;

import lombok.Data;

/**
 * @BelongsProject: yadada-backend
 * @BelongsPackage: com.yage.yadada.model.dto.question
 * @Author: liyajun
 * @CreateTime: 2025-03-02  21:29
 * @Description: TODO
 * @Version: 1.0
 */
@Data
public class QuestionAnswerDTO {

        /*
         *  题目
         * */
        private String title;

        /*
         *  用户答案
         * */
        private String userAnswer;

}
