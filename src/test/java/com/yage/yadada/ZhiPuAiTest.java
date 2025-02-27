package com.yage.yadada;

import com.yage.yadada.config.Aiconfig;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class ZhiPuAiTest {

    @Resource
    private ClientV4 clientV4;


    @Test
    public void test() throws InterruptedException {

        //ClientV4 client = new ClientV4.Builder(KeyConstant.KEY).build();


        // 初始化客户端
//        ClientV4 client = new ClientV4.Builder("af79be0c76564518ac50d9bd7461208d.e2imsxe7Fir41vbR")
//                .build();
        // 构建请求
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "讲讲deepseek");
        messages.add(chatMessage);
//        String requestId = String.format(requestIdTemplate, System.currentTimeMillis());
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4V)
                .stream(Boolean.FALSE)
                .invokeMethod(Constants.invokeMethod)
                .messages(messages)
                .build();
        ModelApiResponse invokeModelApiResp = clientV4.invokeModelApi(chatCompletionRequest);
        System.out.println("model output:" + invokeModelApiResp.getData().getChoices().get(0));
    }
}