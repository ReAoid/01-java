package com.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HashSet;
import java.util.Set;

/**
 * 任务管理器
 * 管理可中断的异步任务，使用CompletableFuture和ExecutorService
 */
@Service
public class TaskManager {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);
    
    // 线程池用于执行异步任务
    private final ExecutorService executorService;
    
    // 存储活跃任务的映射表，线程安全
    private final ConcurrentHashMap<String, CompletableFuture<Void>> activeTasks;
    
    // 存储HTTP调用的映射表，用于取消网络请求
    private final ConcurrentHashMap<String, okhttp3.Call> activeHttpCalls;
    
    // 任务ID生成器
    private final AtomicLong taskIdGenerator;
    
    public TaskManager() {
        // 创建固定大小的线程池，根据CPU核心数调整
        int corePoolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(corePoolSize, r -> {
            Thread thread = new Thread(r, "ChatTask-" + System.currentTimeMillis());
            thread.setDaemon(true); // 设置为守护线程
            return thread;
        });
        
        this.activeTasks = new ConcurrentHashMap<>();
        this.activeHttpCalls = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicLong(0);
        
        logger.info("TaskManager初始化完成，线程池大小: {}", corePoolSize);
    }
    
    /**
     * 生成唯一的任务ID
     */
    public String generateTaskId(String sessionId) {
        long taskNumber = taskIdGenerator.incrementAndGet();
        return sessionId + "_task_" + taskNumber + "_" + System.currentTimeMillis();
    }
    
    /**
     * 提交一个可中断的任务
     * @param taskId 任务ID
     * @param task 要执行的任务
     * @return CompletableFuture对象
     */
    public CompletableFuture<Void> submitTask(String taskId, Runnable task) {
        logger.debug("提交任务: {}", taskId);
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                logger.debug("开始执行任务: {}", taskId);
                task.run();
                logger.debug("任务执行完成: {}", taskId);
            } catch (Exception e) {
                if (e instanceof CancellationException) {
                    logger.info("任务被取消: {}", taskId);
                } else {
                    logger.error("任务执行异常: {}", taskId, e);
                }
                throw e;
            }
        }, executorService);
        
        // 将任务添加到活跃任务映射表
        activeTasks.put(taskId, future);
        
        // 任务完成后自动清理
        future.whenComplete((result, throwable) -> {
            activeTasks.remove(taskId);
            if (throwable != null && !(throwable instanceof CancellationException)) {
                logger.error("任务执行失败: {}", taskId, throwable);
            }
        });
        
        return future;
    }
    
    /**
     * 注册HTTP调用
     * @param taskId 任务ID
     * @param call HTTP调用对象
     */
    public void registerHttpCall(String taskId, okhttp3.Call call) {
        if (call != null) {
            activeHttpCalls.put(taskId, call);
            logger.debug("注册HTTP调用: {}", taskId);
        } else {
            logger.warn("❌ 尝试注册null的HTTP调用: {}", taskId);
        }
    }
    
    /**
     * 取消指定的任务
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        boolean cancelled = false;
        
        // 取消HTTP调用
        okhttp3.Call httpCall = activeHttpCalls.remove(taskId);
        if (httpCall != null && !httpCall.isCanceled()) {
            httpCall.cancel();
            logger.debug("取消HTTP调用: {}", taskId);
            cancelled = true;
        } else if (httpCall != null) {
            logger.debug("HTTP调用已被取消: {}", taskId);
        } else {
            logger.debug("未找到HTTP调用: {}", taskId);
        }
        
        // 取消CompletableFuture任务
        CompletableFuture<Void> future = activeTasks.get(taskId);
        if (future != null) {
            boolean futureCancel = future.cancel(true); // 允许中断正在执行的任务
            if (futureCancel) {
                logger.info("成功取消任务: {}", taskId);
                activeTasks.remove(taskId);
                cancelled = true;
            } else {
                logger.warn("任务取消失败，可能已经完成: {}", taskId);
            }
        }
        
        if (!cancelled) {
            logger.warn("未找到要取消的任务: {}", taskId);
        }
        
        return cancelled;
    }
    
    /**
     * 取消会话的所有任务
     * @param sessionId 会话ID
     * @return 取消的任务数量
     */
    public int cancelSessionTasks(String sessionId) {
        logger.debug("取消会话任务: {}", sessionId);
        
        int cancelledCount = 0;
        
        // 收集所有属于该会话的任务ID
        Set<String> sessionTaskIds = new HashSet<>();
        
        // 从activeTasks中找任务
        for (String taskId : activeTasks.keySet()) {
            logger.debug("检查activeTasks中的任务: {}", taskId);
            if (taskId.startsWith(sessionId + "_task_")) {
                sessionTaskIds.add(taskId);
                logger.debug("找到匹配的activeTasks任务: {}", taskId);
            }
        }
        
        // 从activeHttpCalls中找任务
        for (String taskId : activeHttpCalls.keySet()) {
            logger.debug("检查activeHttpCalls中的任务: {}", taskId);
            if (taskId.startsWith(sessionId + "_task_")) {
                sessionTaskIds.add(taskId);
                logger.debug("找到匹配的HTTP调用任务: {}", taskId);
            }
        }
        
        // 取消所有找到的任务
        for (String taskId : sessionTaskIds) {
            if (cancelTask(taskId)) {
                cancelledCount++;
            }
        }
        
        logger.info("会话 {} 共取消了 {} 个任务", sessionId, cancelledCount);
        return cancelledCount;
    }
    
    /**
     * 检查任务是否存在且未被取消
     * @param taskId 任务ID
     * @return 任务是否活跃
     */
    public boolean isTaskActive(String taskId) {
        CompletableFuture<Void> future = activeTasks.get(taskId);
        return future != null && !future.isCancelled() && !future.isDone();
    }
    
    /**
     * 检查任务是否被取消
     * @param taskId 任务ID
     * @return 任务是否被取消
     */
    public boolean isTaskCancelled(String taskId) {
        CompletableFuture<Void> future = activeTasks.get(taskId);
        return future != null && future.isCancelled();
    }
    
    /**
     * 获取活跃任务数量
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }
    
    /**
     * 获取指定会话的活跃任务数量
     */
    public int getSessionActiveTaskCount(String sessionId) {
        return (int) activeTasks.keySet().stream()
                .filter(taskId -> taskId.startsWith(sessionId + "_task_"))
                .count();
    }
    
    /**
     * 清理已完成的任务（通常不需要手动调用，任务完成时会自动清理）
     */
    public void cleanupCompletedTasks() {
        int initialSize = activeTasks.size();
        activeTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        int cleanedCount = initialSize - activeTasks.size();
        if (cleanedCount > 0) {
            logger.debug("清理了 {} 个已完成的任务", cleanedCount);
        }
    }
    
    /**
     * 关闭TaskManager，释放资源
     */
    public void shutdown() {
        logger.info("开始关闭TaskManager...");
        
        // 取消所有活跃任务
        int cancelledTasks = 0;
        for (String taskId : activeTasks.keySet()) {
            if (cancelTask(taskId)) {
                cancelledTasks++;
            }
        }
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("任务未在5秒内完成，强制关闭");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("等待任务完成时被中断", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("TaskManager关闭完成，取消了 {} 个任务", cancelledTasks);
    }
}
