package com.huawei.ais.demo.asr.model;

public enum JobStatus {
    ACCEPTED(0),
    RUNNING(1),
    FINISHED(2),
    FAILED(-1);

    int statusCode;

    JobStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public static JobStatus valueFrom(int statusCode) {
        for (JobStatus jobStatus : JobStatus.values()) {
            if (jobStatus.statusCode == statusCode) {
                return jobStatus;
            }
        }
        return null;
    }

}
