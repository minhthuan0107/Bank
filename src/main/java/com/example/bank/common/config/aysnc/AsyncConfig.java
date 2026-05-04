package com.example.bank.common.config.aysnc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "webhookTaskExecutor")
    public Executor webhookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Số lượng thread tối thiểu luôn sẵn sàng
        executor.setCorePoolSize(10);

        // Số lượng thread tối đa có thể mở rộng
        executor.setMaxPoolSize(50);

        // Số lượng task nằm chờ trong hàng đợi trước khi mở thêm thread mới (đến Max)
        executor.setQueueCapacity(100);

        // Tiền tố tên thread để dễ debug trong Log (ví dụ: webhook-exec-1)
        executor.setThreadNamePrefix("webhook-exec-");

        // Đảm bảo đóng thread pool một cách êm đẹp khi tắt server
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);  // Chỉ cần ít thread vì gửi mail là tác vụ I/O
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500); // Queue lớn để không mất mail nếu gửi dồn dập
        executor.setThreadNamePrefix("mail-exec-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "userCardSyncTaskExecutor")
    public Executor userCardSyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Đồng bộ card theo user thường không cần quá nhiều thread
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);

        // Queue vừa phải, tránh dồn quá nhiều job lock/unlock card
        executor.setQueueCapacity(200);

        executor.setThreadNamePrefix("user-card-sync-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
