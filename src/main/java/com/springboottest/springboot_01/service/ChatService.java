package com.springboottest.springboot_01.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ChatService {
    String getById(Integer id);
    byte[] processFile(MultipartFile file) throws IOException;
    ResponseEntity<byte[]> fileToZip(List<MultipartFile> files) throws IOException;
    CompletableFuture<ResponseEntity<byte[]>> uploadFile(List<MultipartFile> files);

}
