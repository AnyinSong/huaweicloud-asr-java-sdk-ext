package com.huawei.ais.demo.asr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubmitSuccessRes {
    @JsonProperty("job_id")
    String jobId;

    public String getJobId() {
        return jobId;
    }
}
