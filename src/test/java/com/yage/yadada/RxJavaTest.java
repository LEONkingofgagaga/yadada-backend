package com.yage.yadada;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: yadada-backend
 * @BelongsPackage: com.yage.yadada
 * @Author: liyajun
 * @CreateTime: 2025-03-03  16:27
 * @Description: TODO
 * @Version: 1.0
 */
@SpringBootTest
public class RxJavaTest {

   @Test
    public void test() throws InterruptedException {

       //创建数据流
      Flowable<Long> flowable =
                 Flowable.interval(1, TimeUnit.SECONDS)
                .map(i -> i + 1)
                .subscribeOn(Schedulers.io());


       // 订阅Flowable流，并且打印出每个接受到的数字
       flowable
               .observeOn(Schedulers.io())
               .doOnNext(i -> System.out.println("Received: " + i))
               .subscribe();

       //
       Thread.sleep(10000);
   }
}
