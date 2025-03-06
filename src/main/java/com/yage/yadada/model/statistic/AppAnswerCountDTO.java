package com.yage.yadada.model.statistic;

import lombok.Data;

/**
 * @BelongsProject: yadada-backend
 * @BelongsPackage: com.yage.yadada.model.statistic
 * @Author: liyajun
 * @CreateTime: 2025-03-06  09:56
 * @Description: 用户提交答案数统计
 * @Version: 1.0
 */
@Data
public class AppAnswerCountDTO {


    private Long appId;
    /*
    *
    * */
    private Long answerCount;
}
