package com.huawei.ais.demo.asr;

import java.io.File;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Config {

    private String ak;
    private String sk;

    private String obsBucketName;

    private String asrRegion;
    private String asrEndpoint;
    private int asrFormatType;
    private int queryInterval;
    private int connectionTimeout;
    private int connectionRequestTimeout;
    private int socketTimeout;

    private int submitPoolCoreSize;
    private int submitPoolMaxSize;
    private int submitPoolKeepAliveTime;
    private int submitPoolQueueSize;

    private int callbackPoolCoreSize;
    private int callbackPoolMaxSize;
    private int callbackPoolKeepAliveTime;
    private int callbackPoolQueueSize;

    private int retryCallbackTimes;
    private int retryCallbackInterval;

    private Config() {
        init("config.properties");
    }

    private void init(String propertyFilePath) {
        Configurations propertiesConfigs = new Configurations();
        try {
            PropertiesConfiguration propertiesConfig = propertiesConfigs.properties(new File("config.properties"));
            propertiesConfig.setThrowExceptionOnMissing(true);

            int availableProcessors = Runtime.getRuntime().availableProcessors();

            setAk(propertiesConfig.getString("user.ak"));
            setSk(propertiesConfig.getString("user.sk"));
            setObsBucketName(propertiesConfig.getString("service.obs.bucket.name"));

            setAsrRegion(propertiesConfig.getString("service.asr.region", "cn-north-1"));
            setAsrEndpoint(propertiesConfig.getString("service.asr.endpint", "https://ais.cn-north-1.myhuaweicloud.com"));
            setAsrFormatType(propertiesConfig.getInt("service.asr.format", 1));
            setQueryInterval(propertiesConfig.getInt("service.asr.query.interval", 30000));
            setConnectionTimeout(propertiesConfig.getInt("service.asr.conn.timeout", 5000));
            setConnectionRequestTimeout(propertiesConfig.getInt("service.asr.conn.request.timeout", 1000));
            setSocketTimeout(propertiesConfig.getInt("service.asr.socket.timeout", 20000));

            setSubmitPoolCoreSize(propertiesConfig.getInt("submit.pool.core.size", availableProcessors));
            setSubmitPoolMaxSize(propertiesConfig.getInt("submit.pool.max.size", 4 * availableProcessors));
            setSubmitPoolKeepAliveTime(propertiesConfig.getInt("submit.pool.keepalive.seconds", 60));
            setSubmitPoolQueueSize(propertiesConfig.getInt("submit.pool.queue.size", 100));

            setCallbackPoolCoreSize(propertiesConfig.getInt("callback.pool.core.size", 4 * availableProcessors));
            setCallbackPoolMaxSize(propertiesConfig.getInt("callback.pool.max.size", 8 * availableProcessors));
            setCallbackPoolKeepAliveTime(propertiesConfig.getInt("callback.pool.keepalive.seconds", 60));
            setCallbackPoolQueueSize(propertiesConfig.getInt("callback.pool.queue.size", 1000));

            setRetryCallbackTimes(propertiesConfig.getInt("callback.retry.times", 0));
            setRetryCallbackInterval(propertiesConfig.getInt("callback.retry.interval", 30));

        } catch (ConfigurationException e) {
            throw new RuntimeException("config.properties not found.", e);
        }


    }

    public String getAk() {
        return ak;
    }

    private void setAk(String ak) {
        this.ak = ak;
    }

    public String getSk() {
        return sk;
    }

    private void setSk(String sk) {
        this.sk = sk;
    }

    public String getObsBucketName() {
        return obsBucketName;
    }

    private void setObsBucketName(String obsBucketName) {
        this.obsBucketName = obsBucketName;
    }

    public String getAsrRegion() {
        return asrRegion;
    }

    private void setAsrRegion(String asrRegion) {
        this.asrRegion = asrRegion;
    }

    public String getAsrEndpoint() {
        return asrEndpoint;
    }

    private void setAsrEndpoint(String asrEndpoint) {
        this.asrEndpoint = asrEndpoint;
    }

    public int getAsrFormatType() {
        return asrFormatType;
    }

    private void setAsrFormatType(int asrFormatType) {
        this.asrFormatType = asrFormatType;
    }

    public int getQueryInterval() {
        return queryInterval;
    }

    private void setQueryInterval(int queryInterval) {
        this.queryInterval = queryInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    private void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    private void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    private void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getSubmitPoolCoreSize() {
        return submitPoolCoreSize;
    }

    private void setSubmitPoolCoreSize(int submitPoolCoreSize) {
        this.submitPoolCoreSize = submitPoolCoreSize;
    }

    public int getSubmitPoolMaxSize() {
        return submitPoolMaxSize;
    }

    private void setSubmitPoolMaxSize(int submitPoolMaxSize) {
        this.submitPoolMaxSize = submitPoolMaxSize;
    }

    public int getSubmitPoolKeepAliveTime() {
        return submitPoolKeepAliveTime;
    }

    private void setSubmitPoolKeepAliveTime(int submitPoolKeepAliveTime) {
        this.submitPoolKeepAliveTime = submitPoolKeepAliveTime;
    }

    public int getSubmitPoolQueueSize() {
        return submitPoolQueueSize;
    }

    private void setSubmitPoolQueueSize(int submitPoolQueueSize) {
        this.submitPoolQueueSize = submitPoolQueueSize;
    }

    public int getCallbackPoolCoreSize() {
        return callbackPoolCoreSize;
    }

    private void setCallbackPoolCoreSize(int callbackPoolCoreSize) {
        this.callbackPoolCoreSize = callbackPoolCoreSize;
    }

    public int getCallbackPoolMaxSize() {
        return callbackPoolMaxSize;
    }

    private void setCallbackPoolMaxSize(int callbackPoolMaxSize) {
        this.callbackPoolMaxSize = callbackPoolMaxSize;
    }

    public int getCallbackPoolKeepAliveTime() {
        return callbackPoolKeepAliveTime;
    }

    private void setCallbackPoolKeepAliveTime(int callbackPoolKeepAliveTime) {
        this.callbackPoolKeepAliveTime = callbackPoolKeepAliveTime;
    }

    public int getCallbackPoolQueueSize() {
        return callbackPoolQueueSize;
    }

    private void setCallbackPoolQueueSize(int callbackPoolQueueSize) {
        this.callbackPoolQueueSize = callbackPoolQueueSize;
    }

    public int getRetryCallbackTimes() {
        return retryCallbackTimes;
    }

    private void setRetryCallbackTimes(int retryCallbackTimes) {
        this.retryCallbackTimes = retryCallbackTimes;
    }

    public int getRetryCallbackInterval() {
        return retryCallbackInterval;
    }

    private void setRetryCallbackInterval(int retryCallbackInterval) {
        this.retryCallbackInterval = retryCallbackInterval;
    }

    public static Config getInstance() {
        return SingletonConstructor.config;
    }

    public static void main(String[] args) {
        System.out.println(getInstance().getAk());
    }

    static class SingletonConstructor {
        static Config config = new Config();
    }
}
