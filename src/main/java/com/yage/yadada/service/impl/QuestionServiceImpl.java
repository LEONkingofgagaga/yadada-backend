package com.yage.yadada.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yage.yadada.model.entity.Question;
import com.yage.yadada.service.QuestionService;
import com.yage.yadada.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

/**
* @author KingOfDuck
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2025-02-22 16:56:50
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService{

}




