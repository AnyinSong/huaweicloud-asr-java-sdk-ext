package com.huawei.ais.demo.asr.ext;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.huawei.ais.common.AuthInfo;
import com.huawei.ais.common.ProxyHostInfo;
import com.huawei.ais.demo.asr.CommonUtils;
import com.huawei.ais.demo.asr.Config;
import com.huawei.ais.demo.obs.SimpleObsClient;
import com.huawei.ais.sdk.AisAccess;
import com.huawei.ais.sdk.AisAccessWithProxy;

/**
 * 语音识别服务调用工具类，管理两个线程池<p/>
 * - submitJobExecutors：来执行“下载音频-上传OBS-提交OBS地址给云端引擎”任务<br/>
 * - callbackExecutors：用来执行查询“任务结果-回调”任务<p/>
 * 如果在产品中使用AsrServiceUtils，注意在合适的位置调用destroy()方法来销毁线程池
 */
public class AsrServiceUtils {

    private static final Log LOGGER = LogFactory.getLog(AsrServiceUtils.class);

    private static final Config CONFIG = Config.getInstance();

    private AisAccess aisAccessClient;
    private SimpleObsClient simpleObsClient;

    private ExecutorService submitJobExecutors = null;
    private ExecutorService callbackExecutors = null;

    private AsrServiceUtils() {
        init();
    }

    /**
     * 调用语音识别服务
     *
     * @param audioUrl    音频的文件的url
     * @param callbackUrl 识别结束后的回调url
     * @return 音频提交到语音识别引起的任务句柄，任务提交成功后可通过句柄取到云端识别任务的jobId
     */
    public Future<String> callAsrService(String audioUrl, String callbackUrl) {
        return submitJobExecutors.submit(
                new SubmitJobTask(audioUrl, callbackUrl, aisAccessClient, simpleObsClient, callbackExecutors));
    }

    /**
     * 销毁AsrServiceUtils控制的资源
     */
    public void destroy() {
        CallbackTask.destroyCallbackFailedTaskManager();
        CommonUtils.destroyExecutors(submitJobExecutors, "submitJobExecutors");
        CommonUtils.destroyExecutors(callbackExecutors, "callbackExecutors");
    }

    private void init() {

        AuthInfo asrAuthInfo = new AuthInfo(CONFIG.getAsrEndpoint(), CONFIG.getAsrRegion(), CONFIG.getAk(), CONFIG.getSk());
        ProxyHostInfo proxyHostInfo = new ProxyHostInfo("proxycn2.xxx.com", 8080, "", "");

        aisAccessClient = new AisAccess(asrAuthInfo, CONFIG.getConnectionTimeout(), CONFIG.getConnectionRequestTimeout(),
                CONFIG.getSocketTimeout());
        simpleObsClient = new SimpleObsClient(asrAuthInfo);

        //aisAccessClient = new AisAccessWithProxy(asrAuthInfo, proxyHostInfo, CONFIG.getConnectionTimeout(),
        //        CONFIG.getConnectionRequestTimeout(), CONFIG.getSocketTimeout());
        //simpleObsClient = new SimpleObsClient(asrAuthInfo, proxyHostInfo);


        //初始submitJobExecutors
        submitJobExecutors = new ThreadPoolExecutor(
                CONFIG.getSubmitPoolCoreSize(),
                CONFIG.getSubmitPoolMaxSize(),
                CONFIG.getSubmitPoolKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(CONFIG.getSubmitPoolQueueSize()),
                CommonUtils.ThreadFactoryConstructor(true, "asr-sdk-submit-job-%d"),
                new ThreadPoolExecutor.AbortPolicy());

        //初始callbackExecutors
        callbackExecutors = new ThreadPoolExecutor(
                CONFIG.getCallbackPoolCoreSize(),
                CONFIG.getCallbackPoolMaxSize(),
                CONFIG.getCallbackPoolKeepAliveTime(),
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(CONFIG.getCallbackPoolQueueSize()),
                CommonUtils.ThreadFactoryConstructor(true, "asr-sdk-callback-%d"),
                new ThreadPoolExecutor.AbortPolicy());

        CallbackTask.initCallbackFailedTaskManager(callbackExecutors);
        //创建obs桶
        simpleObsClient.createBucket(CONFIG.getObsBucketName());

        LOGGER.info("AsrServiceUtils init successfully.");
    }

    /**
     * 获取AsrServiceUtils实例（单例）
     *
     * @return AsrServiceUtils实例
     */
    public static AsrServiceUtils getInstance() {
        return SingletonConstructor.asrServiceUtils;
    }

    static class SingletonConstructor {
        static AsrServiceUtils asrServiceUtils = new AsrServiceUtils();
    }

}
