package com.springboottest.springboot_01.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class RequestUtil {

    @Value("${myapi.url.chat}")
    private String chatApiUrl;

    @Value("${myapi.url.kb}")
    private String kbApiUrl;

    public String sendChatRequest(String userMessage) {
        // 根据指定格式构建提示
        String prompt = "<指令>你是一位软件开发生产安全员，请根据输入的文档，总结出该文档的技术知识，并满足以下要求。" +
                "要求：" +
                "1.每个技术知识用40-60字表述。" +
                "2.不同技术知识要表述不同内容。" +
                "3.请按照以下格式输出：1.xxx\\n\\n2.xxx\\n\\n3.xxx\\n\\n..." +
                "</指令>" +
                "<输入文档>" +
                userMessage +
                "</输入文档>";

        // 用提示构建请求体
        String requestBody = """
                {
                  "query": "%s",
                  "history": [],
                  "stream": false,
                  "model_name": "chatglm3-6b",
                  "temperature": 0.5,
                  "prompt_name": "default"
                }
                """.formatted(prompt);

        // 发送 POST 请求
        String response = WebClient.create()
                .post()
                .uri(chatApiUrl)
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 阻塞等待响应

        // 处理返回的 JSON 数据
        return response;
    }

    public String sendKBChatRequest(String userMessage) {
        // 用提示构建请求体
        String requestBody = """
                {
                  "query": "%s",
                  "knowledge_base_name": "test1",
                  "top_k": 3,
                  "score_threshold": 0.55,
                  "history": [],
                  "stream": false,
                  "model_name": "chatglm3-6b",
                  "temperature": 0.5,
                  "prompt_name": "caseKL_2"
                }
                """.formatted(userMessage);

        // 发送 POST 请求
        String response = WebClient.create()
                .post()
                .uri(kbApiUrl)
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 阻塞等待响应

        // 处理返回的 JSON 数据
        return response;
    }
}
