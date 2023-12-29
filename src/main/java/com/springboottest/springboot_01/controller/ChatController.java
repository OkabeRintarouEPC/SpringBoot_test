package com.springboottest.springboot_01.controller;

import com.springboottest.springboot_01.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Operation(summary = "Api测试")
    @GetMapping("/{id}")
    @ApiResponse(responseCode = "200", description = "Success!", content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)))
    public String getById(@PathVariable Integer id) {
        return chatService.getById(id);
    }

    @Operation(summary = "Zip测试")
    @PostMapping(value = "/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "200", description = "Success!", content = @Content(mediaType = "application/octet-stream", schema = @Schema(implementation = byte[].class)))
    public ResponseEntity<byte[]> fileToZip(@RequestPart("files") List<MultipartFile> files) throws IOException {
        return chatService.fileToZip(files);
    }

    @Operation(summary = "Redis测试")
    @GetMapping("/redis")
    public String testRedis() {
        try {
            // 设置键值对
            redisTemplate.opsForValue().set("testKey", "Hello, Redis!");

            // 获取键值对
            String value = redisTemplate.opsForValue().get("testKey");

            return "Redis Test Successful. Value: " + value;
        } catch (Exception e) {
            e.printStackTrace();
            return "Redis Test Failed. Error: " + e.getMessage();
        }
    }

    @Operation(summary = "知识库问答")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "200", description = "Success!", content = @Content(mediaType = "application/octet-stream", schema = @Schema(implementation = byte[].class)))
    public CompletableFuture<ResponseEntity<byte[]>> uploadFile(@RequestPart("files") List<MultipartFile> files) {
        return chatService.uploadFile(files);
    }

}
