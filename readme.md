huaweicloud-asr-java-sdk-ext
=

## 概述

基于华为云
[语音识别服务的Java SDK](https://github.com/huaweicloudsdk/ais-sdk/tree/master/java/ais-asr-java-sdk)，
重写了服务调用代码，并新加了调度层来提供如下两个额外能力:
  * 调用入口可传入外网音频url，sdk将自动下载音频并上传到obs，发起语音识别服务调用
  * 调用入口可传入外网回调url，在识别任务完成时自动回调，并传输识别结果
  
  

## 调用入口说明
调用入口为`com.huawei.ais.demo.asr.ext.AsrServiceUtils`类, 此类管理两个线程池
   - submitJobExecutors：用来执行“下载音频-上传OBS-提交OBS地址给云端引擎”任务
   - callbackExecutors：用来执行查询“任务结果-回调”任务

调用步骤为：
   1. 在resource/config.properties中根据实际情况配置ak/sk等参数
   2. 参考下面的AsrServiceUtils类说明调用服务即可

<br/>
AsrServiceUtils有三个公开方法：
      
    /**
     * 获取AsrServiceUtils实例（单例）
     * @return AsrServiceUtils实例
     */
    public static AsrServiceUtils getInstance();
   
    /**
     * 调用语音识别服务
     *
     * @param audioUrl    音频的文件的url
     * @param callbackUrl 识别结束后的回调url
     * @return 音频提交到语音识别引起的任务句柄，任务提交成功后可通过句柄取到云端识别任务的jobId
     */
    public Future<String> callAsrService(String audioUrl, String callbackUrl); 
   
    /**
     * 销毁AsrServiceUtils控制的资源
     */
    public void destroy();
       
如果在产品中使用AsrServiceUtils，注意在合适的位置调用destroy()方法来销毁线程池。
调用示例可参考`com.huawei.ais.demo.asr.ext.AsrServiceUtilsTest`
