package com.yage.yadada.mapper;

import com.yage.yadada.model.entity.UserAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yage.yadada.model.statistic.AppAnswerCountDTO;
import com.yage.yadada.model.statistic.AppAnswerResultCountDTO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author KingOfDuck
* @description 针对表【user_answer(用户答题记录)】的数据库操作Mapper
* @createDate 2025-02-22 16:56:50
* @Entity generator.domain.UserAnswer
*/
public interface UserAnswerMapper extends BaseMapper<UserAnswer> {
    @Select("select appId, count(userId) as answerCount from user_answer\n" +
            "    group by appId order by answerCount desc limit 10;")
    List<AppAnswerCountDTO> doAppAnswerCount();


    @Select("select resultName, count(resultName) as resultCount from user_answer\n" +
            "    where appId = #{appId}\n" +
            "    group by resultName order by resultCount desc;")
    List<AppAnswerResultCountDTO> doAppAnswerResultCount(Long appId);
}




