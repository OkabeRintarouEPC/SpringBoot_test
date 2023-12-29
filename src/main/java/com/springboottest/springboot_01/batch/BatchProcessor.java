package com.springboottest.springboot_01.batch;

import com.springboottest.springboot_01.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class BatchProcessor {

    @Autowired
    private BatchService batchService;

    // 安排批处理方法每天定时运行
    @Scheduled(cron = "0 0 0 * * ?")
    public void processBatchFilesAtScheduledTime() {
        batchService.processBatchFilesAtScheduledTime();
    }


}
