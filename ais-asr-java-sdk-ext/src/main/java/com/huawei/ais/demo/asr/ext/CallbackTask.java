package com.huawei.ais.demo.asr.ext;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;

import com.huawei.ais.demo.HttpJsonDataUtils;
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
            Object result = queryJobResult(audioUrl, jobId);
            callback(audioUrl, callbackUrl, jobId, result);
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
                LOGGER.error(String.format("Job[%s] failed, %s, associated audio_url:%s",
                        jobId, jobResult.getStatusMsg(), audioUrl));
                return jobResult;
            } else if (jobStatus == JobStatus.FINISHED) {
                // 任务处理成功，打印结果
                LOGGER.info(String.format("Job[%s] finished!", jobId));
                return jobResult;
            } else {
                LOGGER.info("Should not be here!");
            }
        }
        return null;
    }

    private void callback(String audioUrl, String callbackUrl, String jobId, Object result) throws IOException {
        Header[] headers = new Header[]{
                new BasicHeader("Content-Type", "application/json")};

        HttpResponse response = HttpClientUtils.post(callbackUrl, headers,
                HttpJsonDataUtils.objectToHttpEntity(result), CONFIG.getConnectionTimeout(),
                CONFIG.getConnectionRequestTimeout(), CONFIG.getSocketTimeout());

        if (!HttpJsonDataUtils.isOKResponded(response)) {
            LOGGER.error(String.format("Callback for job[%s] failed, associated audio_url:%s", jobId, audioUrl));
            LOGGER.debug("Request body:" + HttpJsonDataUtils.objectToJsonString(result));
            LOGGER.error(HttpJsonDataUtils.responseToString(response));
        } else {
            LOGGER.info(String.format("Callback for job[%s] done.", jobId));
        }
    }
}
