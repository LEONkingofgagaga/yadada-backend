package com.yage.yadada.config;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: yadada-backend
 * @BelongsPackage: com.yage.yadada.config
 * @Author: liyajun
 * @CreateTime: 2025-02-27  15:23
 * @Description: TODO
 * @Version: 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class Aiconfig
{
    /*
    * apikey
    * */
    private String apikey;

    @Bean
    public ClientV4 getClientV4()
    {
        return new ClientV4.Builder(apikey)
                .networkConfig(30 ,60, 60, 60 , TimeUnit.SECONDS)
                .build();
    }
}
