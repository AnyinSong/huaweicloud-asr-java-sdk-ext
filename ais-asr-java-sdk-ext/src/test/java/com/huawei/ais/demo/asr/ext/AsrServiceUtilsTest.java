package com.huawei.ais.demo.asr.ext;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsrServiceUtilsTest {

    @Test
    public void test() throws IOException, InterruptedException {
        AsrServiceUtils asrServiceUtils = AsrServiceUtils.getInstance();

        Future<String> future = asrServiceUtils.callAsrService("http://47.92.133.100:8089/video/2006%E5%B9%B4VA0.mp3",
                "http://127.0.0.1:8080/result/notification");

        Future<String> future2 = asrServiceUtils.callAsrService("http://47.92.133.100:8089/video/2002%E5%B9%B4VA0.mp3",
                "http://127.0.0.1:8080/result/notification");

        //获取JobId的动作是阻塞性的，在音频为提交到云端的语音识别引起前不会返回
        //System.out.println("-------------jobId1=" + future1.get());
        //System.out.println("-------------jobId2=" + future2.get());

        //模拟进程运行一段时间后被停止，释放资源
        Thread.sleep(TimeUnit.MILLISECONDS.convert(6, TimeUnit.MINUTES));
        asrServiceUtils.destroy();
    }
}
