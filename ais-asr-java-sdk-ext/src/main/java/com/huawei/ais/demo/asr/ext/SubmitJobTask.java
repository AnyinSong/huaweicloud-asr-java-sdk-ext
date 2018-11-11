package com.huawei.ais.demo.asr.ext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import com.huawei.ais.demo.HttpJsonDataUtils;
import com.huawei.ais.demo.asr.Config;
import com.huawei.ais.demo.asr.model.SubmitReq;
import com.huawei.ais.demo.asr.model.SubmitSuccessRes;
import com.huawei.ais.demo.obs.ObsFileHandle;
import com.huawei.ais.demo.obs.SimpleObsClient;
import com.huawei.ais.sdk.AisAccess;

class SubmitJobTask implements Callable<String> {

    private static final Log LOGGER = LogFactory.getLog(SubmitJobTask.class);

    private static final String SUBMIT_JOB_URI = "/v1.0/voice/asr/long-sentence";
    private static final String JSON_ROOT = "result";

    private static final Config CONFIG = Config.getInstance();

    private String audioUrl;
    private String callbackUrl;
    private AisAccess aisAccessClient;
    private SimpleObsClient simpleObsClient;
    private ExecutorService callbackExecutors;

    SubmitJobTask(String audioUrl, String callbackUrl, AisAccess aisAccessClient, SimpleObsClient simpleObsClient,
                  ExecutorService callbackExecutors) {
        this.audioUrl = audioUrl;
        this.callbackUrl = callbackUrl;
        this.aisAccessClient = aisAccessClient;
        this.simpleObsClient = simpleObsClient;
        this.callbackExecutors = callbackExecutors;
    }

    @Override
    public String call() throws IOException {
        String filePath = downloadAudio(audioUrl);
        ObsFileHandle obsFileHandle = simpleObsClient.uploadFile(CONFIG.getObsBucketName(), filePath);
        String jobId = null;
        try {
            jobId = submitJobToAsrService(audioUrl, obsFileHandle.generateSharedDownloadUrl());
            if (jobId != null) {
                LOGGER.info(String.format("Create callback task for job[%s].", jobId));
                callbackExecutors.submit(new CallbackTask(audioUrl, jobId, callbackUrl, aisAccessClient));
                return jobId;
            }
        } catch (RejectedExecutionException e1) {
            LOGGER.error(String.format("Submit callback task failed. job_id=%s audio_url=%s.", jobId, audioUrl));
        }
        LOGGER.error(String.format("Submit job to asr service failed for audio[%s].", audioUrl));
        return null;
    }

    private String downloadAudio(String audioUrl) throws IOException {
        LOGGER.info("Begin to download audio file... url:" + audioUrl);
        try {
            URL url = new URL(audioUrl);
            String urlDecoded = URLDecoder.decode(audioUrl, "UTF-8");
            String fileName = urlDecoded.substring(urlDecoded.lastIndexOf("/") + 1);
            File destFile = new File("data/" + fileName);
            FileUtils.copyURLToFile(url, destFile);
            LOGGER.info("Download done! local:" + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();
        } catch (IOException e) {
            LOGGER.error("Download audio failed. audio_url:" + audioUrl, e);
            throw e;
        }
    }

    private String submitJobToAsrService(String audio, String obsUrl) throws IOException {
        SubmitReq submitReq = new SubmitReq();
        submitReq.setUrl(obsUrl);

        HttpResponse response = aisAccessClient.post(SUBMIT_JOB_URI, HttpJsonDataUtils.objectToHttpEntity(submitReq));
        if (!HttpJsonDataUtils.isOKResponded(response)) {
            LOGGER.error(String.format("Submit the job failed, audio_url:%s obs_url:%s", audio, obsUrl));
            LOGGER.debug("Request body:" + HttpJsonDataUtils.objectToPrettyJsonString(submitReq));
            String responseStr = EntityUtils.toString(response.getEntity(), "UTF-8");
            LOGGER.error(responseStr);

        }

        // 获取到提交成功的任务ID, 准备进行结果的查询
        SubmitSuccessRes submitResult = HttpJsonDataUtils.getResponseObject(response, SubmitSuccessRes.class, JSON_ROOT);
        String jobId = submitResult.getJobId();
        LOGGER.info("Submit job done, job_id=" + jobId);
        return jobId;
    }
}
