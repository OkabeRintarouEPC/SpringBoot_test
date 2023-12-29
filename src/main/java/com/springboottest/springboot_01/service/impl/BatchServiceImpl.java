package com.springboottest.springboot_01.service.impl;

import com.springboottest.springboot_01.service.BatchService;
import com.springboottest.springboot_01.util.RequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.springboottest.springboot_01.util.FileUtil.*;

@Service
public class BatchServiceImpl implements BatchService {

    @Autowired
    private RequestUtil requestUtil;

    // 根据需要调整线程池大小
    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 得到日志对象
    private static Logger logger = LoggerFactory.getLogger(ChatServiceImpl.class);

    @Override
    public void processBatchFilesAtScheduledTime() {
        // 处理 "input" 文件夹中的文件
        String inputFolderPath = "./input"; // 根据需要更新路径

        try {
            // 读取文件夹中的文件并转换为字节数组的列表
            List<byte[]> files = readFilesFromFolder(inputFolderPath);

            CompletableFuture<List<byte[]>> batchProcessingResult = processBatchFiles(files);

            // 等待批量处理完成
            List<byte[]> resultBytesList = batchProcessingResult.get();

            // 在批量处理完成后可以执行其他操作

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.info("发生错误: {}！", e.getMessage());
        }
    }

    private CompletableFuture<List<byte[]>> processBatchFiles(List<byte[]> files) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 存储每个文件处理的CompletableFuture的列表
                List<CompletableFuture<byte[]>> processingFutures = files.stream()
                        .map(fileBytes -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return processFileBytes(fileBytes);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, executorService))
                        .collect(Collectors.toList());

                // 在所有CompletableFuture完成后处理结果
                List<byte[]> resultBytes = processingFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());

                // 将每个处理过的DOCX文件保存到输出文件夹
                saveBatchFilesLocally(resultBytes);

                return resultBytes;
            } catch (IOException e) {
                e.printStackTrace();
                logger.info("发生错误: {}！", e.getMessage());
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    private byte[] processFileBytes(byte[] fileBytes) throws IOException {
        // 打印文件名
        logger.info("Processing File...");

        // 读取文件到 userMessage
        String userMessage = readDocxFile(new ByteArrayInputStream(fileBytes));

        // 调用 chat 功能
        String response = requestUtil.sendChatRequest(userMessage);

        // 显示返回的数据
        logger.info("Response from server: " + response);

        // 处理返回的数据
        String finalResult = processResponse(response, requestUtil);

        // 保存响应到新的 docx 文件
        return saveResponseToDocx(finalResult, generateFileNameWithDateTime("output_doc"));
    }

}
