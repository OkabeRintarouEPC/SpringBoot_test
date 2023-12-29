package com.springboottest.springboot_01.service.impl;

import com.springboottest.springboot_01.service.ChatService;
import com.springboottest.springboot_01.util.RequestUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.springboottest.springboot_01.util.FileUtil.*;
import static com.springboottest.springboot_01.util.FileUtil.generateFileNameWithDateTime;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private RequestUtil requestUtil;

//    @Autowired
//    private RedisTemplate<String, String> redisTemplate; // 引入 RedisTemplate

    // 根据需要调整线程池大小
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 得到日志对象
    private static Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    public String getById(Integer id) {
        logger.info("id ==> "+id);
        return "hello , spring boot!";

    }

    @Override
    public byte[] processFile(MultipartFile file) throws IOException {
        // 打印文件名
        logger.info("Uploaded Filename: " + file.getOriginalFilename());

        // 读取上传文件到userMessage
        String userMessage = readDocxFile(file.getInputStream());

        // 显示userMessage
        logger.info("User Message: " + userMessage);

        // 调用chat功能
        String response = requestUtil.sendChatRequest(userMessage);

        // 显示返回的数据
        logger.info("Response from server: " + response);

        // 处理返回的数据
        String finalResult = processResponse(response, requestUtil);

        // 保存响应到新的docx文件
        byte[] docxBytes = saveResponseToDocx(finalResult, generateFileNameWithDateTime("src/main/response_doc"));

        return docxBytes;
    }


    @Override
    public ResponseEntity<byte[]> fileToZip(List<MultipartFile> files) throws IOException {
        try {
            List<byte[]> docxBytesList = new ArrayList<>();

            // 将MultipartFile对象转换为字节数组
            for (MultipartFile file : files) {
                byte[] docxBytes = file.getBytes();
                docxBytesList.add(docxBytes);
            }
            byte[] zipBytes = createZipFile(docxBytesList);
            // 设置报头并返回ZIP文件
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "response.zip");

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("发生错误: {}！", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public CompletableFuture<ResponseEntity<byte[]>> uploadFile(List<MultipartFile> files) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 存储每个处理线程中CompletableFuture对象的List
                List<CompletableFuture<byte[]>> processingFutures = files.stream()
                        .map(file -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return processFile(file);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, executorService))
                        .collect(Collectors.toList());

                // 在所有CompletableFuture对象执行完毕后处理结果
                List<byte[]> resultBytes = processingFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());

                // 创建一个包含所有处理过的docx文件的zip文件
                byte[] zipBytes = createZipFile(resultBytes);

                saveZipLocally(zipBytes, "./response.zip");

                // 设置报头并返回ZIP文件
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "response.zip");

                return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("发生错误: {}！", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }, executorService);
    }


}
