package com.huawei.ais.demo.asr;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CommonUtils {

    private static final Log LOGGER = LogFactory.getLog(CommonUtils.class);

    /**
     * 销毁线程池
     *
     * @param executorService 线程池对象
     * @param desc            线程池描述
     */
    public static void destroyExecutors(ExecutorService executorService, String desc) {
        if (executorService != null) {
            LOGGER.info(String.format("Shutdown thread pool[%s]", desc));
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                    LOGGER.error(String.format("Wait for all tasks in pool[%s] finish in timeout(1 min), call shutdownNow().", desc));
                    executorService.shutdownNow();
                } else {
                    LOGGER.info(String.format("Terminate thread pool[%s] of asr successfully", desc));
                }
            } catch (InterruptedException e) {
                LOGGER.error(String.format("Wait for all tasks in pool[%s] finish interrupted, call shutdownNow().", desc));
                executorService.shutdownNow();
            }
        }
    }

    /**
     * 线程工厂构造器
     *
     * @param daemon           是否设置为守护线程
     * @param threadNameFormat 线程名称pattern，eg.“xx-xx-thread-%d”
     * @return ThreadFactory实例
     */
    public static ThreadFactory ThreadFactoryConstructor(boolean daemon, String threadNameFormat) {
        return new ThreadFactory() {
            final AtomicLong count = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setDaemon(daemon);
                thread.setName(String.format(threadNameFormat, count.getAndIncrement()));
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        LOGGER.error("Uncaught exception in thread " + t.getName(), e);
                    }
                });
                return thread;
            }
        };
    }
}
