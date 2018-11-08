package com.huawei.ais.demo.asr.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Category {
    @JsonProperty("common")
    COMMON,
    @JsonProperty("dialog")
    DIALOG
}
