package com.sectrend.buildscan.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sectrend.buildscan.exception.WfpException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Callable;
import org.springframework.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;
@Configuration
public class ThreadPoolUtils {

    private final static Logger log = LoggerFactory.getLogger(ThreadPoolUtils.class);

    public Executor createExecutor(String poolSize, String maxPoolSize, String queueCapacity, String threadNamePrefix) {
        //配置核心线程数
        int asyncPoolSize = 10;
        //配置最大线程数
        int asyncMaxPoolSize = 20;
        //配置队列大小
        int asyncQueueCapacity = 500;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutorWrapper();
        try {
            if (StringUtils.isNotBlank(poolSize) && !"0".equals(poolSize.replace(" ", ""))) {
                String replacePoolSize = poolSize.replace(" ", "");
                asyncPoolSize = Integer.parseInt(replacePoolSize);
            }
            if (StringUtils.isNotBlank(maxPoolSize) && !"0".equals(maxPoolSize.replace(" ", ""))) {
                String maxReplace = maxPoolSize.replace(" ", "");
                asyncMaxPoolSize = Integer.parseInt(maxReplace);
            }
            if (StringUtils.isNotBlank(queueCapacity) && !"0".equals(queueCapacity.replace(" ", ""))) {
                String queueCapacityReplace = queueCapacity.replace(" ", "");
                asyncQueueCapacity = Integer.parseInt(queueCapacityReplace);
            }
            if(asyncPoolSize < 0 || asyncMaxPoolSize < 0 || asyncQueueCapacity < 0){
                throw new WfpException("Thread data cannot be negative");
            }
        } catch (Exception e) {
            log.error("Fingerprint thread number type error: WfpKernelThreadSize：{}, WfpMaxThreadSize：{}, WfpQueueCapacity：{}, {}", poolSize, maxPoolSize, queueCapacity, e.getMessage());
            System.exit(12);
        }

        if (asyncPoolSize - asyncMaxPoolSize >= 0) {
            log.error("The number of core lines cannot be greater than or equal to the maximum number of threads： poolSize：{}, maxPoolSize：{}", asyncPoolSize, asyncMaxPoolSize);
            System.exit(12);
        }

        executor.setCorePoolSize(asyncPoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        //配置线程池中的线程的名称前缀
        executor.setThreadNamePrefix("async-" + threadNamePrefix);
        // rejection-policy：当pool已经达到max size的时候，如何处理新任务
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //执行初始化
        executor.initialize();

        return executor;
    }

    public class ThreadPoolTaskExecutorWrapper extends ThreadPoolTaskExecutor {

        private void logThreadPoolMetrics(String operationType) {
            ThreadPoolExecutor threadPoolExecutor = getThreadPoolExecutor();
            log.debug(" Operation: {}, ThreadNamePrefix: {}, taskCount [{}], completedTaskCount [{}], activeCount [{}], queueSize [{}], poolSize [{}]",
                    operationType,
                    this.getThreadNamePrefix(),
                    threadPoolExecutor.getTaskCount(),
                    threadPoolExecutor.getCompletedTaskCount(),
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getQueue().size(),
                    threadPoolExecutor.getPoolSize());
        }

        @Override
        public void execute(Runnable task) {
            logThreadPoolMetrics("execute(Runnable)");
            super.execute(task);
        }

        @Override
        public void execute(Runnable task, long startTimeout) {
            logThreadPoolMetrics("execute(Runnable, long)");
            super.execute(task, startTimeout);
        }

        @Override
        public Future<?> submit(Runnable task) {
            logThreadPoolMetrics("submit(Runnable)");
            return super.submit(task);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            logThreadPoolMetrics("submit(Callable<T>)");
            return super.submit(task);
        }

        @Override
        public ListenableFuture<?> submitListenable(Runnable task) {
            logThreadPoolMetrics("submitListenable(Runnable)");
            return super.submitListenable(task);
        }

        @Override
        public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
            logThreadPoolMetrics("submitListenable(Callable<T>)");
            return super.submitListenable(task);
        }
    }
}
