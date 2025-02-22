package com.yage.yadada.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yage.yadada.model.entity.App;
import com.yage.yadada.service.AppService;
import com.yage.yadada.mapper.AppMapper;
import org.springframework.stereotype.Service;

/**
* @author KingOfDuck
* @description 针对表【app(应用)】的数据库操作Service实现
* @createDate 2025-02-22 16:56:50
*/
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>
    implements AppService{

}




