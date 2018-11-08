package com.huawei.ais.demo.asr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetResultRes {
    @JsonProperty("status_code")
    int statusCode;
    @JsonProperty("status_msg")
    String statusMsg;
    @JsonProperty("words")
    String words;

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMsg() {
        return statusMsg;
    }

    public String getWords() {
        return words;
    }
}
