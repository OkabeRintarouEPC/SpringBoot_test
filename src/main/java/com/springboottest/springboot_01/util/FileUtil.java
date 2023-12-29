package com.springboottest.springboot_01.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class FileUtil {

    private static RedisTemplate<String, String> redisTemplate;

    @Autowired
    public FileUtil(RedisTemplate<String, String> redisTemplate) {
        FileUtil.redisTemplate = redisTemplate;
    }

    private static Logger logger = LoggerFactory.getLogger(FileUtil.class);
    public static String readDocxFile(InputStream inputStream) throws IOException {
        XWPFDocument document = new XWPFDocument(inputStream);

        // 读取文档中的文本内容
        StringBuilder content = new StringBuilder();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                content.append(run.getText(0));
            }
        }
        return content.toString();
    }


    public static List<byte[]> readFilesFromFolder(String folderPath) throws IOException {
        List<byte[]> files = new ArrayList<>();
        // 从文件夹中读取所有docx文档
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".docx")) {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    files.add(fileBytes);
                }
            }
        }

        return files;
    }

    public static String generateFileNameWithDateTime(String prefix) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return prefix + "_" + now.format(formatter) + ".docx";
    }

    public static byte[] saveResponseToDocx(String response, String filePath) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(filePath)) {

            // 创建段落和运行对象
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            // 按行分割响应文本并设置段落内容，保留换行
            String[] lines = response.split("\n");
            for (int i = 0; i < lines.length; i++) {
                run.setText(lines[i]);
                if (i < lines.length - 1) {
                    run.addCarriageReturn(); // 添加换行符
                }
            }

            // 保存到文件
            document.write(fos);
        }
        // 读取文件的字节数组
        byte[] docxBytes = Files.readAllBytes(Paths.get(filePath));

        // 删除文件
        Files.deleteIfExists(Paths.get(filePath));

        return docxBytes;
    }

    public static String processResponse(String response, RequestUtil requestUtil) {
        // 拆分响应并去除前后空格
        String[] responseParts = response.split("\n");
        StringBuffer resultBuffer = new StringBuffer();

        // 使用 CompletableFuture 异步处理每个部分
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (String part : responseParts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isEmpty()) {
                continue;
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                // 调用发送 HTTP 请求的功能
                String partResponse = requestUtil.sendKBChatRequest(part);

                // 显示返回的部分数据
                logger.info(partResponse);

                // 获取响应中的 answer 字段
                String resultPart = extractAnswerFromJson(partResponse);

                // 存储数据到 Redis
                String key = generateKey(trimmedPart);
                redisTemplate.opsForValue().set(key, resultPart);

            });

            futures.add(future);
        }

        // 等待所有 CompletableFuture 完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        allOf.join();

        // 从 Redis 读取数据进行拼接
        for (String part : responseParts) {
            String trimmedPart = part.trim();
            if (trimmedPart.isEmpty()) {
                continue;
            }
            String key = generateKey(trimmedPart);
            String resultPart = redisTemplate.opsForValue().get(key);

            resultBuffer.append(resultPart).append("\n");
        }

        return resultBuffer.toString();
    }

    // 将多个docx文档打包成zip文件
    public static byte[] createZipFile(List<byte[]> docxBytesList) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (int i = 0; i < docxBytesList.size(); i++) {
                byte[] docxBytes = docxBytesList.get(i);
                String entryName = generateFileNameWithDateTime("response_doc_" + (i + 1) );

                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(docxBytes);
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        }
    }

    // 保存包含处理过的文档的zip文件到指定路径
    public static void saveZipLocally(byte[] zipBytes, String localPath) throws IOException {
        Path path = Path.of(localPath);
        Files.write(path, zipBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    // 保存处理过的文档到输出文件夹
    public static void saveBatchFilesLocally(List<byte[]> resultBytesList) throws IOException {
        for (int i = 0; i < resultBytesList.size(); i++) {
            byte[] docxBytes = resultBytesList.get(i);
            String fileName = generateFileNameWithDateTime("output_doc_" + (i + 1));

            String filePath = "./output/" + fileName; // 根据需要更新路径

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(docxBytes);
            }
        }
    }

    private static String extractAnswerFromJson(String jsonResponse) {
        // 解析 JSON 响应
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // 获取 answer 字段
        String answer = jsonObject.getString("answer");

        return answer;
    }

    // 生成用于Redis Key的方法，可以根据需要修改
    private static String generateKey(String part) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String hash = DigestUtils.md5Hex(part);
        return "responseData:" + now.format(formatter) + ":" + "md5_" + hash;
    }

}
