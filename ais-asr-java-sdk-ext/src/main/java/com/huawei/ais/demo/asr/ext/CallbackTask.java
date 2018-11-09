package com.huawei.ais.demo.asr.ext;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.huawei.ais.demo.HttpJsonDataUtils;
import com.huawei.ais.demo.asr.CommonUtils;
import com.huawei.ais.demo.asr.Config;
import com.huawei.ais.demo.asr.model.GetResultRes;
import com.huawei.ais.demo.asr.model.JobStatus;
import com.huawei.ais.sdk.AisAccess;
import com.huawei.ais.sdk.util.HttpClientUtils;

class CallbackTask implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(CallbackTask.class);

    private static final Config CONFIG = Config.getInstance();

    private static final String GET_JOB_RESULT_URI_TEMPLATE = "/v1.0/voice/asr/long-sentence?job_id=%s&format="
            + CONFIG.getAsrFormatType();

    private static final String JSON_ROOT = "result";
    private static final long QUERY_JOB_RESULT_INTERVAL = CONFIG.getQueryInterval();

    //Map<任务，还需要重试的次数>
    private static Map<CallbackTask, RetryRecord> callbackFailedTasks = new ConcurrentHashMap<>();
    private static ScheduledExecutorService retryCallbackExecutor;
    private static ExecutorService callbackExecutors;

    private String audioUrl;
    private String jobId;
    private String callbackUrl;
    private AisAccess aisAccessClient;

    CallbackTask(String audioUrl, String jobId, String callbackUrl, AisAccess aisAccessClient) {
        this.audioUrl = audioUrl;
        this.jobId = jobId;
        this.callbackUrl = callbackUrl;
        this.aisAccessClient = aisAccessClient;
    }

    @Override
    public void run() {
        try {
            int retryTimes = 0;
            if (callbackFailedTasks.containsKey(this)) {
                int retriedTimes = callbackFailedTasks.get(this).getRetriedTimes();
                retryTimes = retriedTimes + 1;
                LOGGER.info(String.format("Retry[%d/%d] callback for job[%s]", retryTimes, CONFIG.getRetryCallbackTimes(), jobId));
            }

            Object result = queryJobResult(audioUrl, jobId);
            boolean callbackSuccess = callback(audioUrl, callbackUrl, jobId, result);

            if (callbackSuccess) {
                callbackFailedTasks.remove(this);
            } else {
                if (retryTimes >= CONFIG.getRetryCallbackTimes()) {
                    LOGGER.error(String.format("Retry[%d/%d] callback for job[%s], give up!", retryTimes,
                            CONFIG.getRetryCallbackTimes(), jobId));
                    callbackFailedTasks.remove(this);
                } else {
                    LOGGER.error(String.format("Callback failed for job[%s], will try later.", jobId));
                    callbackFailedTasks.put(this, new RetryRecord(retryTimes));
                }
            }
        } catch (IOException e) {
            LOGGER.error("");
        }
    }

    private Object queryJobResult(String audioUrl, String jobId) throws IOException {

        // 构建进行查询的请求链接，并进行轮询查询，由于是异步任务，必须多次进行轮询
        // 直到结果状态为任务已处理结束
        String url = String.format(GET_JOB_RESULT_URI_TEMPLATE, jobId);
        while (!Thread.currentThread().isInterrupted()) {
            HttpResponse getResponse = aisAccessClient.get(url);
            if (!HttpJsonDataUtils.isOKResponded(getResponse)) {
                LOGGER.error(String.format("Query job[%s] result failed, associated audio_url:%s", jobId, audioUrl));
                LOGGER.info(HttpJsonDataUtils.responseToString(getResponse));
                break;
            }
            GetResultRes jobResult
                    = HttpJsonDataUtils.getResponseObject(getResponse, GetResultRes.class, JSON_ROOT);
            JobStatus jobStatus = JobStatus.valueFrom(jobResult.getStatusCode());

            // 根据任务状态觉得继续轮询或者打印结果
            if (jobStatus == JobStatus.ACCEPTED || jobStatus == JobStatus.RUNNING) {
                //如果任务还未处理完，等待一段时间，继续进行轮询
                LOGGER.info(String.format("Job[%s] %s, waiting...", jobId, jobResult.getStatusMsg()));
                try {
                    Thread.sleep(QUERY_JOB_RESULT_INTERVAL);
                } catch (InterruptedException e) {
                    LOGGER.warn(String.format("Thread[%s] was interrupted, exit.", Thread.currentThread().getName()));
                }
            } else if (jobStatus == JobStatus.FAILED) {
                // 如果处理失败，直接退出
                LOGGER.error(String.format("Job[%s] has failed, %s, associated audio_url:%s",
                        jobId, jobResult.getStatusMsg(), audioUrl));
                return jobResult;
            } else if (jobStatus == JobStatus.FINISHED) {
                // 任务处理成功
                LOGGER.info(String.format("Job[%s] has finished.", jobId));
                return jobResult;
            } else {
                LOGGER.info("Should not be here!");
            }
        }
        return null;
    }

    private boolean callback(String audioUrl, String callbackUrl, String jobId, Object result) throws IOException {
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", ContentType.APPLICATION_JSON.toString())};

        HttpResponse response = HttpClientUtils.post(callbackUrl, headers,
                HttpJsonDataUtils.objectToHttpEntity(new Notification(jobId, result)), CONFIG.getConnectionTimeout(),
                CONFIG.getConnectionRequestTimeout(), CONFIG.getSocketTimeout());

        if (!HttpJsonDataUtils.isOKResponded(response)) {
            LOGGER.error(String.format("Callback for job[%s] failed, associated audio_url:%s", jobId, audioUrl));
            LOGGER.debug("Request body:" + HttpJsonDataUtils.objectToJsonString(result));
            LOGGER.error(HttpJsonDataUtils.responseToString(response));
            return false;
        } else {
            LOGGER.info(String.format("Callback for job[%s] done.", jobId));
            return true;
        }
    }

    public String getJobId() {
        return this.jobId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CallbackTask that = (CallbackTask) o;
        return Objects.equals(jobId, that.jobId);
    }

    protected static void initCallbackFailedTaskManager(ExecutorService callbackExecutors) {
        CallbackTask.callbackExecutors = callbackExecutors;
        retryCallbackExecutor = Executors.newSingleThreadScheduledExecutor(
                CommonUtils.ThreadFactoryConstructor(true, "asr-sdk-retry-callback-%d"));

        retryCallbackExecutor.scheduleAtFixedRate(
                new FailedCallbackTasksScanner(),
                CONFIG.getRetryCallbackInterval(),
                CONFIG.getRetryCallbackInterval() / 3,
                TimeUnit.SECONDS);
    }

    protected static void destroyCallbackFailedTaskManager() {
        CommonUtils.destroyExecutors(retryCallbackExecutor, "retryCallbackExecutor");
        callbackFailedTasks.clear();
    }

    static class FailedCallbackTasksScanner implements Runnable {
        @Override
        public void run() {
            long nowInSeconds;
            for (Map.Entry<CallbackTask, RetryRecord> entry : callbackFailedTasks.entrySet()) {
                nowInSeconds = TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
                //只有没有处于没有正在重试且距上次重试间隔等于或超过设定门限状态的回调task才会被再次提交到回调任务池中
                if (!entry.getValue().isRetrying()
                        && nowInSeconds - entry.getValue().getLatestRetryTime() >= CONFIG.getRetryCallbackInterval()) {
                    try {
                        LOGGER.info(String.format("Submit retry callback task for job[%s].", entry.getKey().getJobId()));
                        callbackExecutors.submit(entry.getKey());
                        entry.getValue().setRetrying(true);
                    } catch (RejectedExecutionException e) {
                        LOGGER.error(String.format("Failed to submit retry callback task for job[%s] failed, try later.",
                                entry.getKey().getJobId()), e);
                    }
                }
            }
        }
    }


    static class RetryRecord {
        private int retriedTimes;
        private long latestRetryTime;
        private boolean isRetrying;

        RetryRecord(int retriedTimes) {
            this.retriedTimes = retriedTimes;
            this.latestRetryTime = TimeUnit.SECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
            this.isRetrying = false;
        }

        int getRetriedTimes() {
            return retriedTimes;
        }

        long getLatestRetryTime() {
            return latestRetryTime;
        }

        boolean isRetrying() {
            return isRetrying;
        }

        void setRetrying(boolean retrying) {
            isRetrying = retrying;
        }
    }

    static class Notification {
        @JsonProperty("job_id")
        String jobId;
        @JsonProperty("asr_result")
        Object resultFromEngine;

        public Notification(String jobId, Object resultFromEngine) {
            this.jobId = jobId;
            this.resultFromEngine = resultFromEngine;
        }
    }
}
