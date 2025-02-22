package com.yage.yadada.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yage.yadada.model.entity.UserAnswer;
import com.yage.yadada.service.UserAnswerService;
import com.yage.yadada.mapper.UserAnswerMapper;
import org.springframework.stereotype.Service;

/**
* @author KingOfDuck
* @description 针对表【user_answer(用户答题记录)】的数据库操作Service实现
* @createDate 2025-02-22 16:56:50
*/
@Service
public class UserAnswerServiceImpl extends ServiceImpl<UserAnswerMapper, UserAnswer>
    implements UserAnswerService{

}




